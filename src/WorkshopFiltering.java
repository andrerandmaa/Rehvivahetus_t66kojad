import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;

public class WorkshopFiltering {

    private List<List<String>> allAvailableTimes = new ArrayList<>();
    private boolean refreshXML = true;

    public boolean isRefreshXML() {
        return refreshXML;
    }

    public void setRefreshXML(boolean refreshXML) {
        this.refreshXML = refreshXML;
    }

    public List<List<String>> getAllAvailableTimes() {
        return allAvailableTimes;
    }

    public void setAllAvailableTimes(List<List<String>> allAvailableTimes) {
        this.allAvailableTimes = allAvailableTimes;
    }

    public JButton FilterButton(JFrame frame, List<JCheckBoxMenuItem> selectedWorkShops, List<JDateChooser> startAndEndDates, List<JCheckBox> selectedVehicleTypes, Map<String, List<List<String>>> fileContents, DefaultTableModel tableModel, List<String> bookingIdList, GridBagConstraints gbc) {
        List<List<List<String>>> allAvailableTimes = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        JButton submitButton = new JButton("Otsi aegasid");

        submitButton.addActionListener(e -> {
            // Clears all previous information from available times table
            allAvailableTimes.clear();
            bookingIdList.clear();
            tableModel.setRowCount(0);

            // Reformat start and end dates into easily comparable form
            String startDate = "";
            String endDate = "";
            if (startAndEndDates.getFirst().getDate() != null) {
                startDate = dateFormat.format(startAndEndDates.getFirst().getDate());
            }
            if (startAndEndDates.getLast().getDate() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startAndEndDates.getLast().getDate());
                calendar.add(Calendar.DAY_OF_MONTH, 1); // add 1 day to search results

                Date modifiedEndDate = calendar.getTime();
                endDate = dateFormat.format(modifiedEndDate);
            }

            boolean workShopSelected = false;
            boolean vehicleTypeSelected = false;
            boolean startAndEndDatesEntered = false;
            boolean isStartDateBeforeEndDate = false;

            // Go through all selected workshops
            for (JCheckBoxMenuItem selectedWorkShop : selectedWorkShops) {
                if (selectedWorkShop.isSelected()) {
                    workShopSelected = true;

                    // searches through workshop names and saved information from tireWorkshops.txt
                    for (String workshop : fileContents.keySet()) {
                        if (selectedWorkShop.getText().equals(workshop)) {

                            // Go through all selected serivceable vehicle types
                            for (JCheckBox selectedVehicleType : selectedVehicleTypes) {
                                if (selectedVehicleType.isSelected()) {
                                    vehicleTypeSelected = true;

                                    if (!startDate.isEmpty() && !endDate.isEmpty()) {
                                        startAndEndDatesEntered = true;

                                        if (!startAndEndDates.getLast().getDate().before(startAndEndDates.getFirst().getDate())) {
                                            isStartDateBeforeEndDate = true;

                                            // confirms if workshop services selected vehicle type
                                            if (fileContents.get(workshop).get(1).contains(selectedVehicleType.getText())) {
                                                String apiURL = fileContents.get(workshop).get(2).getFirst();
                                                // searches which url to use for API
                                                if (apiURL.contains("v1")) {
                                                    RestApiXML restApiXML = new RestApiXML();
                                                    try {
                                                        List<List<String>> availableTimes = restApiXML.RestApiGet(apiURL, startDate, endDate, workshop);
                                                        allAvailableTimes.add(availableTimes);
                                                    } catch (URISyntaxException | IOException |
                                                             InterruptedException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                } else if (apiURL.contains("v2")) {
                                                    RestApiJSON restApiJSON = new RestApiJSON();
                                                    try {
                                                        List<List<String>> availableTimes = restApiJSON.RestApiGet(apiURL, startDate, endDate, workshop);
                                                        allAvailableTimes.add(availableTimes);
                                                    } catch (URISyntaxException | IOException |
                                                             InterruptedException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                } else {
                                                    throw new RuntimeException("Unknown REST API url");
                                                }
                                                break;

                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
            if (!workShopSelected) {
                JOptionPane.showMessageDialog(frame, "Vali vähemalt 1 töökoda!");
            } else if (!vehicleTypeSelected) {
                JOptionPane.showMessageDialog(frame, "Vali vähemalt 1 sõiduki tüüp!");
            } else if (!startAndEndDatesEntered) {
                JOptionPane.showMessageDialog(frame, "Vali algus- ja lõppkuupäev!");
            } else if (!isStartDateBeforeEndDate) {
                JOptionPane.showMessageDialog(frame, "Algus kuupäev peab olema varem kui lõpp kuupäev!");
            }
            else {
                List<List<String>> sortedAvailableTimeList = sortByDate(allAvailableTimes);
                if (sortedAvailableTimeList.isEmpty() && isRefreshXML()) {
                    JOptionPane.showMessageDialog(frame, "Ühtegi vaba aega ei leitud!");
                }
                else {
                    setRefreshXML(true); // restore normal button functionality
                    setAllAvailableTimes(sortedAvailableTimeList);
                    FilteredResults(tableModel, fileContents, bookingIdList);
                }
            }
        });

        gbc.insets = new Insets(30, 36, 0, 0);
        submitButton.setPreferredSize(new Dimension(120, 30));
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;

        return submitButton;
    }

    // All available booking times are rearranged to: earliest -> latest
    private List<List<String>> sortByDate(List<List<List<String>>> allAvailableTimes) {
        List<List<String>> flattenedList = new ArrayList<>(allAvailableTimes.stream()
                .flatMap(List::stream)
                .toList());

        flattenedList.sort(new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                ZonedDateTime dateTime1 = ZonedDateTime.parse(o1.get(2));
                ZonedDateTime dateTime2 = ZonedDateTime.parse(o2.get(2));
                return dateTime1.compareTo(dateTime2);
            }
        });

        return flattenedList;
    }

    // Adds found results to available times table
    private void FilteredResults(DefaultTableModel tableModel, Map<String, List<List<String>>> fileContents, List<String> bookingIdList) {
        for (List<String> singleAvailableTime : getAllAvailableTimes()) {
            String workshopName = singleAvailableTime.getFirst();
            String workshopAddress = String.join("", fileContents.get(singleAvailableTime.getFirst()).getFirst());
            String id = singleAvailableTime.get(1);

            // Splits date and time into more readable format
            String[] dateTime = singleAvailableTime.get(2).split("T");
            String date = dateTime[0];
            String time = dateTime[1].substring(0, dateTime[1].length() - 1);

            String servicedVehicleTypes = String.join(", ", fileContents.get(singleAvailableTime.getFirst()).get(1));

            bookingIdList.add(id);

            List<String> formattedList = new ArrayList<>();
            formattedList.add(workshopName);
            formattedList.add(workshopAddress);
            formattedList.add(date + "   " + time);
            formattedList.add(servicedVehicleTypes);

            tableModel.addRow(formattedList.toArray());
        }
    }
}
