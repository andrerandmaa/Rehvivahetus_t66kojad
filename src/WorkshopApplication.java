import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class WorkshopApplication extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WorkshopApplication().setVisible(true));
    }

    public WorkshopApplication() {

        // Read in the file, which has all workshop information
        FileReader fileReader = new FileReader();
        Map<String, List<List<String>>> fileContents = fileReader.readFile("tireWorkshops.txt");

        List<String> workshopList = new ArrayList<>(fileContents.keySet());

        // Main frame
        setTitle("Rehvitöökojad");
        setMinimumSize(new Dimension(700, 600));
        setSize(700, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Main frame has 3 sectors: north, center, south
        // North panel with grid
        JPanel northPanel = new JPanel(new GridBagLayout());
        northPanel.setBackground(new Color(129, 225, 255));
        northPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        //frame.setResizable(false);

        // Northpanel interactions for all selectable filters
        List<JCheckBoxMenuItem> selectedWorkShops = WorkShopSelectionButton(workshopList, gbc, northPanel);
        List<JDateChooser> startAndEndDates = StartDateEndDate(gbc, northPanel);
        List<JCheckBox> selectedVehicleTypes = VehicleTypeSelection(gbc, northPanel);

        String[] columnNames = {"Töökoja nimi", "Aadress", "Aeg", "Teenindatavad sõidukid"};

        // All available booking times are added into tableModel. Forbid cell editing
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        // tableModel takes up center sector
        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(0, 5, 5, 5));
        scrollPane.setBackground(new Color(129, 225, 255));
        add(scrollPane, BorderLayout.CENTER);

        List<String> bookingIdList = new ArrayList<>();

        // Button that finds all available booking times based on filters. In northpanel
        WorkshopFiltering workshopFiltering = new WorkshopFiltering();
        JButton filterButton = workshopFiltering.FilterButton(this, selectedWorkShops, startAndEndDates, selectedVehicleTypes, fileContents, tableModel, bookingIdList, gbc);

        northPanel.add(filterButton, gbc);
        add(northPanel, BorderLayout.NORTH);

        // Find all row details AND id for later api calls
        String[] selectedRowId = new String[1];
        List<String> selectedRowDetails = availableWorkshopsTimesTable(table, tableModel, bookingIdList, selectedRowId);

        // All functions related to booking a time. In South panel
        bookSelectedTime(this, fileContents, selectedRowDetails, filterButton, workshopFiltering, tableModel);
    }

    private void bookSelectedTime(JFrame frame, Map<String, List<List<String>>> fileContents, List<String> selectedRowDetails, JButton filterButton, WorkshopFiltering workshopFiltering, DefaultTableModel tableModel) {
        // Booking button in south panel
        JPanel southPanel = new JPanel();
        JButton selectTimeslotButton = new JButton("Broneeri aeg");
        selectTimeslotButton.setPreferredSize(new Dimension(120, 30));
        southPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        southPanel.setBackground(new Color(129, 225, 255));
        southPanel.add(selectTimeslotButton);
        frame.add(southPanel, BorderLayout.SOUTH);

        // Booking button functionality
        selectTimeslotButton.addActionListener(e -> {
            // Open pop-up window
            if (tableModel.getRowCount() != 0) {
                if (!selectedRowDetails.isEmpty()) {
                    showPopup(frame, selectedRowDetails, fileContents, filterButton, workshopFiltering);
                } else {
                    JOptionPane.showMessageDialog(frame, "Vali aeg tabelist!");
                }
            }
            else {
                JOptionPane.showMessageDialog(frame, "Otsi esmalt aegasid!");
            }
        });
    }

    private void showPopup(JFrame frame, List<String> selectedRowDetails, Map<String, List<List<String>>> fileContents, JButton filterButton, WorkshopFiltering workshopFiltering) {
        // Pop-up window design
        JDialog popupDialog = new JDialog(frame, "Kinnituse aken", true);
        popupDialog.setSize(360, 220);
        popupDialog.setResizable(false);
        popupDialog.setLocationRelativeTo(frame);
        popupDialog.setLayout(new BorderLayout());

        // North panel
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());

        JLabel messageLabel = new JLabel("      Veendu töökoja andmetes:", JLabel.LEFT);
        messagePanel.add(messageLabel, BorderLayout.NORTH);

        // Center panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        // Create smaller-sized informational text
        JLabel infoLabelName = new JLabel("      Töökoja nimi: " + selectedRowDetails.get(1));
        JLabel infoLabelAddress = new JLabel("      Aadress: " + selectedRowDetails.get(2));
        JLabel infoLabelTime = new JLabel("      Millal: " + selectedRowDetails.get(3));
        JLabel infoLabelVehicleType = new JLabel("      Teenindatavad sõidukid: " + selectedRowDetails.get(4));

        Font plainFont = new Font(infoLabelName.getFont().getName(), Font.PLAIN, 12);
        infoLabelName.setFont(plainFont);
        infoLabelAddress.setFont(plainFont);
        infoLabelTime.setFont(plainFont);
        infoLabelVehicleType.setFont(plainFont);

        // Add informational labels to the panel
        infoPanel.add(infoLabelName);
        infoPanel.add(infoLabelAddress);
        infoPanel.add(infoLabelTime);
        infoPanel.add(infoLabelVehicleType);

        messagePanel.add(infoPanel, BorderLayout.CENTER);

        // Text field for user contact info input
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 18, 10, 18)); // Add some padding

        JLabel textFieldMessage = new JLabel("Sisesta oma kontaktinfo:", JLabel.LEFT);
        textPanel.add(textFieldMessage);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(600, 25));
        textField.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(textField);

        messagePanel.add(textPanel, BorderLayout.SOUTH);

        // Create buttons, in south panel
        JButton cancelButton = new JButton("Välju");
        JButton confirmButton = new JButton("Kinnita");
        confirmButton.setEnabled(false); // At start disable the confirm button

        // Panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);

        // Add action listeners to the buttons
        // Cancel can be used at any time
        cancelButton.addActionListener(e -> {
            popupDialog.dispose();   // Close the pop-up window on cancel
        });

        // Confirm button functionality for booking selected time
        confirmButton.addActionListener(e -> {
            boolean requestAnswerFound = apiRequests(selectedRowDetails, fileContents, textField.getText());
            if (requestAnswerFound) {
                JOptionPane.showMessageDialog(popupDialog, "Aeg broneeritud!");
                workshopFiltering.setRefreshXML(false); // To not show pop-up window about "Not finding any times"
                filterButton.doClick(); // Refresh the available bookings table
                popupDialog.dispose();
                selectedRowDetails.clear();
            }
            else {
                JOptionPane.showMessageDialog(popupDialog, "Midagi läks broneerimisel valesti");
                popupDialog.dispose();
            }
        });

        // Confirm button requires at least 1 character from user in text field
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtonState();
            }

            private void updateButtonState() {
                // Enable the confirm button if the text field is not empty
                confirmButton.setEnabled(!textField.getText().trim().isEmpty());
            }
        });

        // Add components to the pop-up frame
        popupDialog.add(messagePanel, BorderLayout.CENTER);
        popupDialog.add(buttonPanel, BorderLayout.SOUTH);

        popupDialog.setVisible(true);
    }

    private boolean apiRequests(List<String> selectedRowDetails, Map<String, List<List<String>>> fileContents, String contactInfo) {
        // Looks at id construction of selected row from available times table
        if (selectedRowDetails.getFirst().contains("-")) {
            for (String workshop : fileContents.keySet()) {
                String apiURL = fileContents.get(workshop).get(2).getFirst();
                // looks at apiURL (comes from tireWorkshops.txt)
                if (apiURL.contains("v1")) {
                    RestApiXML restApiXML = new RestApiXML();
                    try {
                        // Books tire change time
                        restApiXML.RestApiPost(apiURL, selectedRowDetails.getFirst(), contactInfo);
                    } catch (URISyntaxException | IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    return true;
                }
            }
        }
        else {
            for (String workshop : fileContents.keySet()) {
                String apiURL = fileContents.get(workshop).get(2).getFirst();
                if (apiURL.contains("v2")) {
                    RestApiJSON restApiJSON = new RestApiJSON();
                    try {
                        restApiJSON.RestApiPut(apiURL, selectedRowDetails.getFirst(), contactInfo);
                    } catch (URISyntaxException | IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> availableWorkshopsTimesTable(JTable table, DefaultTableModel tableModel, List<String> bookingIdList, String[] selectedRowId) {
        List<String> selectedRowDetails = new ArrayList<>();

        // Add a row selection listener
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        selectedRowDetails.clear();

                        selectedRowId[0] = bookingIdList.get(selectedRow);
                        String name = (String) tableModel.getValueAt(selectedRow, 0);
                        String address = (String) tableModel.getValueAt(selectedRow, 1);
                        String timestamp = (String) tableModel.getValueAt(selectedRow, 2);
                        String vehicleTypes = (String) tableModel.getValueAt(selectedRow, 3);

                        selectedRowDetails.add(selectedRowId[0]);
                        selectedRowDetails.add(name);
                        selectedRowDetails.add(address);
                        selectedRowDetails.add(timestamp);
                        selectedRowDetails.add(vehicleTypes);
                    }
                }
            }
        });

        return selectedRowDetails;
    }


    private List<JDateChooser> StartDateEndDate(GridBagConstraints gbc, JPanel northPanel) {

        JLabel chooseDateLabel = new JLabel("Otsi kuupäevi:");
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        northPanel.add(chooseDateLabel, gbc);

        gbc.insets = new Insets(10, 20, 5, 10);

        JLabel startDateLabel = new JLabel("Algus: ");
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        northPanel.add(startDateLabel, gbc);

        gbc.insets = new Insets(10, 0, 5, 10);

        JDateChooser startDateChooser = new JDateChooser();
        startDateChooser.getDateEditor().setEnabled(false);
        startDateChooser.setPreferredSize(new Dimension(120, 30));
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        northPanel.add(startDateChooser, gbc);

        gbc.insets = new Insets(10, 20, 5, 10);

        JLabel endDateLabel = new JLabel("Lõpp: ");
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        northPanel.add(endDateLabel, gbc);

        gbc.insets = new Insets(10, 0, 5, 10);

        JDateChooser endDateChooser = new JDateChooser();
        endDateChooser.getDateEditor().setEnabled(false);
        endDateChooser.setPreferredSize(new Dimension(120, 30));
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        northPanel.add(endDateChooser, gbc);

        // Updates first selectable date in calendar based on actual current day
        Runnable updateSelectableDateRange = getRunnable(startDateChooser, endDateChooser);

        // Run updateSelectableDateRange every day at midnight (so program can be kept running for days without restart)
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(updateSelectableDateRange);
            }
        }, getInitialDelay(), 24 * 60 * 60 * 1000); // 24 hours

        // Add a property change listener to startDateChooser
        startDateChooser.addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Date startDate = resetTime(startDateChooser.getDate());
                Date endDate = resetTime(endDateChooser.getDate());

                // If startDate is later than endDate, then clear EndDate
                 if (startDate != null && endDate != null && startDate.after(endDate)) {
                    endDateChooser.setDate(null);
                }
                // Set the selectable date range of endDateChooser starting from startDate
                endDateChooser.setSelectableDateRange(startDate, null);
            }
        });

        List<JDateChooser> dates = new ArrayList<>();
        dates.add(startDateChooser);
        dates.add(endDateChooser);

        return dates;
    }

    private static Runnable getRunnable(JDateChooser startDateChooser, JDateChooser endDateChooser) {
        Runnable updateSelectableDateRange = () -> {
            Date currentDate = new Date();
            startDateChooser.setSelectableDateRange(currentDate, null);
            endDateChooser.setSelectableDateRange(currentDate, null);

            Date startDate = startDateChooser.getDate();
            // End date can't be before start date
            if (startDate != null && !startDate.before(currentDate)) {
                endDateChooser.setSelectableDateRange(startDate, null);
            }
        };

        // For updating
        updateSelectableDateRange.run();
        return updateSelectableDateRange;
    }

    // Helper method to set time to 00:00:00
    private static Date resetTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    // Helper method to calculate the initial delay until the next midnight
    private static long getInitialDelay() {
        Calendar now = Calendar.getInstance();
        Calendar nextMidnight = (Calendar) now.clone();
        nextMidnight.add(Calendar.DAY_OF_MONTH, 1);
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 0);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);
        return nextMidnight.getTimeInMillis() - now.getTimeInMillis();
    }

    // Checkboxes for serviceable vehicle types
    private List<JCheckBox> VehicleTypeSelection(GridBagConstraints gbc, JPanel northPanel) {
        gbc.insets = new Insets(10, 45, 5, 10);

        JLabel vehicleSelectionLabel = new JLabel("Vali sõiduki tüübid:");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        northPanel.add(vehicleSelectionLabel, gbc);

        JCheckBox carCheckBox = new JCheckBox("Sõiduauto");
        carCheckBox.setFont(new Font(null, Font.PLAIN, 16));
        carCheckBox.setBackground(new Color(129, 225, 255));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        northPanel.add(carCheckBox, gbc);

        JCheckBox truckCheckBox = new JCheckBox("Veoauto");
        truckCheckBox.setFont(new Font(null, Font.PLAIN, 16));
        truckCheckBox.setBackground(new Color(129, 225, 255));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        northPanel.add(truckCheckBox, gbc);

        List<JCheckBox> selectedVehicleTypes = new ArrayList<>();
        selectedVehicleTypes.add(carCheckBox);
        selectedVehicleTypes.add(truckCheckBox);

        return selectedVehicleTypes;
    }

    // Everything related to workshop chooser button and drop-down menu
    private List<JCheckBoxMenuItem> WorkShopSelectionButton(List<String> workshopList, GridBagConstraints gbc, JPanel northPanel) {
        JLabel workshopLabel = new JLabel("Rehvitöökojad: ");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 25, 5, 30);
        northPanel.add(workshopLabel, gbc);

        JButton button = new JButton("-VALI-");
        button.setPreferredSize(new Dimension(120, 30));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 5, 30);
        northPanel.add(button, gbc);

        JPopupMenu dropdownMenu = new JPopupMenu();

        // Create a scroll pane to hold the menu items
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(150, 200));

        // Creates dropdown menu
        Object[] workshopValues = WorkshopDropDownMenu(workshopList, dropdownMenu, button);
        JPanel menuPanel = (JPanel) workshopValues[0];
        List<JCheckBoxMenuItem> selectedWorkshops = (List<JCheckBoxMenuItem>) workshopValues[1];

        scrollPane.setViewportView(menuPanel);
        dropdownMenu.add(scrollPane);

        // Close dropdown menu when leaving dropdownMenu area
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                dropdownMenu.setVisible(false);
            }
        });

        // Add action listener to the button to toggle popup menu visibility
        button.addActionListener(e -> {
            if (!dropdownMenu.isVisible()) {
                dropdownMenu.show(button, 0, button.getHeight());
            }
        });

        northPanel.add(button, gbc);
        return selectedWorkshops;
    }

    private Object[] WorkshopDropDownMenu(List<String> workshopList, JPopupMenu dropdownMenu, JButton button) {
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        List<JCheckBoxMenuItem> menuItems = new ArrayList<>();

        // Holds all names that are displayed in button getText()
        List<String> selectedWorkShopNamesForButton = new ArrayList<>();

        // Create "Select All" checkbox menu item
        JCheckBoxMenuItem selectAllItem = SelectAllButtonFunction(menuItems, dropdownMenu, selectedWorkShopNamesForButton, button);
        menuPanel.add(selectAllItem);

        // Adding all selectable workshops to dropdown menu (originate from tireWorkshops.txt)
        for (String workshopName : workshopList) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(workshopName);
            menuItems.add(menuItem);
            menuPanel.add(menuItem);

            // So that multiple workshops can be chosen without closing down dropdown menu after each click
            menuItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);

            // Keep the menu open when mouse enters a menu item
            menuItem.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    dropdownMenu.setVisible(true);
                }
            });

            // Menuitem selection logic
            menuItem.addActionListener(e -> {
                boolean selected = menuItem.isSelected();
                boolean setBool = true;

                // When unselecting a workshop
                if (!selected) {
                    selectAllItem.setSelected(false);
                    setBool = false;
                    selectedWorkShopNamesForButton.remove(menuItem.getText());
                }
                else {
                    for (JCheckBoxMenuItem item : menuItems) {
                        // If even one workshop is unchosen, don't activate "-KÕIK-"
                        if (!item.isSelected()) {
                            setBool = false;
                        }
                        // Add all selected workshops to list
                        else if (!selectedWorkShopNamesForButton.contains(item.getText())) {
                            selectedWorkShopNamesForButton.add(item.getText());
                        }
                    }
                    // Select "-KÕIK-"
                    if (setBool) {
                        selectAllItem.setSelected(true);
                        button.setText(selectAllItem.getText());
                    }
                }

                if (!setBool) {
                    // No workshops chosen
                    if (selectedWorkShopNamesForButton.isEmpty()) {
                        button.setText("-VALI-");
                    } else {
                        // Format: (+number)WorkshopName
                        if (selectedWorkShopNamesForButton.size() > 1) {
                            button.setText("(+" + (selectedWorkShopNamesForButton.size() - 1) + ")" + selectedWorkShopNamesForButton.getFirst());
                        }
                        // Format: WorkshopName
                        else {
                            button.setText(selectedWorkShopNamesForButton.getFirst());
                        }
                    }
                }
            });
        }

        return new Object[]{menuPanel, menuItems};
    }

    private JCheckBoxMenuItem SelectAllButtonFunction(List<JCheckBoxMenuItem> menuItems, JPopupMenu popupMenu, List<String> selectedWorkShopNamesForButton, JButton button) {
        // Functionality for "-KÕIK-" or "Select all" button
        JCheckBoxMenuItem selectAllItem = new JCheckBoxMenuItem("-KÕIK-");
        selectAllItem.addActionListener(e -> {
            boolean selected = selectAllItem.isSelected();
            for (JCheckBoxMenuItem menuItem : menuItems) {
                menuItem.setSelected(selected); // Sets all dropdown menu workshops selection mode according to "-KÕIK-"
                if (selected && !selectedWorkShopNamesForButton.contains(menuItem.getText())) {
                    selectedWorkShopNamesForButton.add(menuItem.getText());
                }
            }

            if (selected) {
                button.setText(selectAllItem.getText());
            }
            // unselected "-KÕIK-"
            else {
                button.setText("-VALI-");
                selectedWorkShopNamesForButton.clear();
            }
        });

        // Keep the menu open when mouse enters a menu item
        selectAllItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                popupMenu.setVisible(true); // Keep the menu open when mouse enters a menu item
            }
        });
        selectAllItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);

        return selectAllItem;
    }
}