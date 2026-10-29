import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;


public class NetQueue {
    
    private static final Color BACKGROUND = new Color(28, 29, 32);
    private static final Color PANEL = new Color(36, 37, 41);
    private static final Color PANEL_DARK = new Color(31, 32, 36);
    private static final Color BORDER = new Color(63, 65, 70);
    private static final Color TEXT = new Color(232, 232, 229);
    private static final Color MUTED = new Color(151, 153, 158);
    private static final Color BLUE = new Color(76, 125, 194);
    private static final Color GREEN = new Color(93, 170, 132);
    private static final Color RED = new Color(202, 104, 112);
    private static final Color AMBER = new Color(202, 157, 76);
    private static final Color TEAL = new Color(64, 192, 190);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String WEB_API_URL = "https://netqueue.jingexz01.space";
        private static final String WEB_API_TOKEN = System.getenv("NETQUEUE_PUSH_TOKEN");
        private static final HttpClient WEB_API_CLIENT = HttpClient.newHttpClient();

    
    private final JPanel queuePanel = new JPanel();
    private final JLabel queueCount = new JLabel("0 in queue");
    private final JLabel footer = new JLabel("Ready");
    private final QueueModel.LinkedQueue<QueueModel.Customer> waitingQueue = new QueueModel.LinkedQueue<>();
    private final QueueModel.Stack<Allocation> allocationHistory = new QueueModel.Stack<>();
    private final QueueModel.DynamicArray<String> activityLog = new QueueModel.DynamicArray<>();
    private final QueueModel.DynamicArray<Station> stationCatalog = new QueueModel.DynamicArray<>();
    private final QueueModel.DynamicArray<StationSlot> availableStations = new QueueModel.DynamicArray<>();
    private final QueueModel.HashTable<String, StationSlot> terminalIndex = new QueueModel.HashTable<>();
    private final QueueModel.HashTable<String, String> reservationIndex = new QueueModel.HashTable<>();
    private final QueueModel.BinarySearchTree<String> waitingCustomerIndex = new QueueModel.BinarySearchTree<>();
    private final QueueModel.Graph<String> stationNetwork = new QueueModel.Graph<>();
    private JFrame frame;
    private Timer webHeartbeat;
    private JPanel selectionBar;
    private JLabel selectionLabel;
    private StationSlot selectedStation;
    private JLabel averageWaitValue;
    private JLabel activeValue;
    private JLabel stationSummary;
    private JLabel openSummary;
    private JLabel inUseSummary;
    private JLabel offlineSummary;
    private JLabel servedValue;
    private JLabel todayWaitValue;
    private JLabel peakValue;
    private JLabel throughputValue;
    private JLabel cpuValue;
    private JLabel memoryValue;
    private JLabel networkValue;
    private int servedCount;
    private int peakLoad;

    
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.width", 10);
        SwingUtilities.invokeLater(() -> new NetQueue().show());
    }

    private void show() {
        
        frame = new JFrame("NetQueue | Control Center");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1100, 700));
        frame.setSize(1365, 760);
        frame.setLocationByPlatform(true);
        frame.setContentPane(buildWindow());
        frame.setVisible(true);
        webHeartbeat = new Timer(15_000, event -> pushSnapshot());
        webHeartbeat.start();
    }

    private JPanel buildWindow() {
        
        JPanel root = panel(BACKGROUND);
        root.setLayout(new BorderLayout(0, 1));
        root.setBackground(BACKGROUND);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        refreshDashboard();
        return root;
    }

    private JPanel buildHeader() {
        
        JPanel header = panel(BACKGROUND);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setPreferredSize(new Dimension(0, 64));
        header.setLayout(new BorderLayout(24, 0));
        JPanel brand = row(16);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        brand.add(label("NetQueue", 16, TEXT));
        brand.add(label("Control Center", 13, MUTED));
        brand.add(separator());
        brand.add(label("●  All systems operational", 12, GREEN));
        header.add(brand, BorderLayout.WEST);
        JPanel actions = row(16);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));
        actions.add(metric("0 min", "avg wait"));
        actions.add(metric("0/0", "active"));
        actions.add(separator());
        JButton allocate = button("Auto-Allocate Next", BLUE, Color.WHITE, true);
        allocate.addActionListener(this::allocateNext);
        actions.add(allocate);
        JButton undo = button("Undo", PANEL_DARK, TEXT, false);
        undo.addActionListener(e -> undoAllocation());
        actions.add(undo);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        
        JPanel body = panel(BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.setLayout(new GridBagLayout());
        addGrid(body, buildLeftRail(), 0, 0, 0, 1, 100, 0, GridBagConstraints.NORTHWEST);
        addGrid(body, buildStationArea(), 1, 0, 1, 1, 0, 1, GridBagConstraints.CENTER);
        addGrid(body, buildRightRail(), 2, 0, 0, 1, 270, 0, GridBagConstraints.NORTHEAST);
        return body;
    }

    private void addGrid(JPanel parent, Component child, int x, int y, int weightX,
                         int weightY, int width, int fill, int anchor) {
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightX;
        constraints.weighty = weightY;
        constraints.fill = fill == 1 ? GridBagConstraints.BOTH : GridBagConstraints.VERTICAL;
        constraints.anchor = anchor;
        constraints.insets = new Insets(0, x == 0 ? 0 : 10, 0, x == 2 ? 0 : 0);
        if (width > 0) constraints.ipadx = width;
        parent.add(child, constraints);
    }

    private JPanel buildLeftRail() {
        
        JPanel rail = panel(BACKGROUND);
        rail.setLayout(new GridBagLayout());
        rail.add(buildNewCustomer());
        GridBagConstraints form = constraints(0, 0, 1, 0, GridBagConstraints.HORIZONTAL);
        rail.removeAll();
        rail.add(buildNewCustomer(), form);
        GridBagConstraints waiting = constraints(0, 1, 1, 1, GridBagConstraints.BOTH);
        waiting.insets = new Insets(14, 0, 14, 0);
        rail.add(buildWaiting(), waiting);
        rail.add(buildSummary(), constraints(0, 2, 1, 0, GridBagConstraints.HORIZONTAL));
        return rail;
    }

    private GridBagConstraints constraints(int x, int y, double weightx, double weighty, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightx;
        constraints.weighty = weighty;
        constraints.fill = fill;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        return constraints;
    }

    private JPanel buildNewCustomer() {
        
        JPanel box = card();
        box.setLayout(new GridBagLayout());
        JTextField name = new JTextField();
        name.setToolTipText("Customer name");
        styleField(name, "Full name");
        JToggleButton standardOption = toggleOption("Standard", true);
        JToggleButton vipOption = toggleOption("VIP", false);
        JTextField duration = new JTextField();
        duration.setToolTipText("Session length in minutes");
        styleField(duration, "Duration in minutes");
        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(standardOption);
        typeGroup.add(vipOption);
        JPanel typeToggle = new JPanel(new GridLayout(1, 2, 8, 0));
        typeToggle.setOpaque(false);
        typeToggle.add(standardOption);
        typeToggle.add(vipOption);
        JButton add = button("Add to Queue", BLUE, Color.WHITE, true);
        add.addActionListener(e -> {
            
            String value = name.getText().trim();
            if (value.isEmpty() || value.equals("Full name")) {
                footer.setText("Customer name is required");
                name.requestFocusInWindow();
                return;
            }
            if (waitingCustomerIndex.contains(value)) {
                footer.setText("A customer with that name is already waiting");
                name.requestFocusInWindow();
                return;
            }
            int durationMinutes;
            try {
                durationMinutes = Integer.parseInt(duration.getText().trim());
            } catch (NumberFormatException error) {
                durationMinutes = 0;
            }
            if (durationMinutes < 1 || durationMinutes > 24 * 60) {
                footer.setText("Duration must be between 1 minute and 24 hours");
                duration.requestFocusInWindow();
                return;
            }
            String type = vipOption.isSelected() ? "VIP" : "Standard";
            QueueModel.Customer customer = new QueueModel.Customer(value, type, durationMinutes * 60);
            waitingQueue.enqueue(customer);
            waitingCustomerIndex.insert(customer.name());
            log(customer.name() + " joined the queue");
            name.setText("");
            duration.setText("");
            standardOption.setSelected(true);
            refreshWaiting();
            footer.setText(customer.name() + " added to the queue");
        });
        box.add(label("NEW CUSTOMER", 11, MUTED), constraints(0, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        GridBagConstraints field = constraints(0, 1, 1, 0, GridBagConstraints.HORIZONTAL);
        field.insets = new Insets(14, 0, 0, 0);
        box.add(name, field);
        GridBagConstraints select = constraints(0, 2, 1, 0, GridBagConstraints.HORIZONTAL);
        select.insets = new Insets(12, 0, 0, 0);
        box.add(typeToggle, select);
        GridBagConstraints durationField = constraints(0, 3, 1, 0, GridBagConstraints.HORIZONTAL);
        durationField.insets = new Insets(12, 0, 0, 0);
        box.add(duration, durationField);
        GridBagConstraints action = constraints(0, 4, 1, 0, GridBagConstraints.HORIZONTAL);
        action.insets = new Insets(14, 0, 0, 0);
        box.add(add, action);
        return box;
    }

    
    private JToggleButton toggleOption(String text, boolean selected) {
        JToggleButton toggle = new JToggleButton(text, selected);
        toggle.setFont(new Font("SansSerif", Font.BOLD, 12));
        toggle.setFocusPainted(false);
        toggle.setOpaque(true);
        toggle.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        toggle.putClientProperty("FlatLaf.style", "arc: 8");
        styleToggle(toggle);
        toggle.addItemListener(e -> styleToggle(toggle));
        return toggle;
    }

    private void styleToggle(JToggleButton toggle) {
        if (toggle.isSelected()) {
            toggle.setBackground(BLUE);
            toggle.setForeground(Color.WHITE);
        } else {
            toggle.setBackground(PANEL_DARK);
            toggle.setForeground(MUTED);
        }
    }

    private JPanel buildWaiting() {
        
        JPanel box = card();
        box.setLayout(new BorderLayout(0, 12));
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(label("WAITING", 11, MUTED), BorderLayout.WEST);
        queueCount.setFont(new Font("Monospaced", Font.PLAIN, 11));
        queueCount.setForeground(MUTED);
        title.add(queueCount, BorderLayout.EAST);
        box.add(title, BorderLayout.NORTH);
        queuePanel.setOpaque(false);
        queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.Y_AXIS));
        refreshWaiting();
        box.add(queuePanel, BorderLayout.CENTER);
        return box;
    }

    private void refreshWaiting() {
        
        queuePanel.removeAll();
        QueueModel.DynamicArray<QueueModel.Customer> customers = waitingQueue.snapshot();
        for (int i = 0; i < customers.size(); i++) {
            QueueModel.Customer customer = customers.get(i);
            JPanel item = row(8);
            item.putClientProperty("FlatLaf.style",
                    "arc: 8; borderWidth: 1; borderColor: " + hex(i == 0 ? new Color(63, 112, 96) : BORDER));
            item.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            item.setBackground(i == 0 ? new Color(29, 55, 49) : PANEL_DARK);
            JLabel number = label(String.valueOf(i + 1), 12, i == 0 ? GREEN : MUTED);
            number.setPreferredSize(new Dimension(20, 20));
            item.add(number);
            item.add(label(customer.name(), 12, TEXT));
            item.add(label(customer.type(), 10, "VIP".equals(customer.type()) ? AMBER : MUTED));
            item.add(Box.createHorizontalGlue());
            if (i == 0) item.add(label("NEXT", 10, GREEN));
            queuePanel.add(item);
            queuePanel.add(Box.createVerticalStrut(7));
        }
        queueCount.setText(waitingQueue.size() + " in queue");
        queuePanel.revalidate();
        queuePanel.repaint();
        refreshDashboard();
    }

    private JPanel buildSummary() {
        
        JPanel box = card();
        box.setLayout(new GridLayout(1, 3));
        box.add(summaryValue("0", "open", GREEN));
        box.add(summaryValue("0", "in use", RED));
        box.add(summaryValue("0", "offline", AMBER));
        return box;
    }

    private JPanel buildStationArea() {
        
        JPanel area = panel(BACKGROUND);
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        area.setLayout(new BorderLayout(0, 14));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(label("Zone Monitor", 17, TEXT));
        titles.add(Box.createVerticalStrut(4));
        stationSummary = label("0 stations \u00b7 live", 12, MUTED);
        titles.add(stationSummary);
        heading.add(titles, BorderLayout.WEST);
        heading.add(legend(), BorderLayout.EAST);
        area.add(heading, BorderLayout.NORTH);
        JPanel groups = panel(BACKGROUND);
        groups.setLayout(new GridBagLayout());
        Station[] vipStations = new Station[]{
                new Station("VIP-01", "Available", "No active session"),
                new Station("VIP-02", "Available", "No active session"),
                new Station("VIP-03", "Maintenance", "System Update", "Station in maintenance"),
                new Station("VIP-04", "Available", "No active session")
        };
        addStations(vipStations);
        groups.add(group("VIP ZONE \u00b7 4 STATIONS", true, vipStations), constraints(0, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        Station[] standardStations = new Station[]{
                new Station("STD-01", "Available", "No active session"),
                new Station("STD-02", "Available", "No active session"),
                new Station("STD-03", "Available", "No active session"),
                new Station("STD-04", "Maintenance", "Driver Install", "Station in maintenance"),
                new Station("STD-05", "Available", "No active session"),
                new Station("STD-06", "Available", "No active session"),
                new Station("STD-07", "Available", "No active session"),
                new Station("STD-08", "Available", "No active session")
            };
            addStations(standardStations);
        groups.add(group("STANDARD ZONE \u00b7 8 STATIONS", false, standardStations), constraints(0, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        JScrollPane scroll = new JScrollPane(groups);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        area.add(scroll, BorderLayout.CENTER);
        selectionBar = buildSelectionBar();
        area.add(selectionBar, BorderLayout.SOUTH);
        return area;
    }

    private JPanel group(String title, boolean vip, Station[] stations) {
        
        JPanel group = panel(vip ? new Color(26, 36, 48) : BACKGROUND);
        Color accent = vip ? TEAL : BORDER;
        group.setLayout(new BorderLayout(0, 12));
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, vip ? 3 : 0, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, vip ? 13 : 0, 12, 0)));
        JLabel titleLabel = label(title, 11, vip ? TEAL : MUTED);
        titleLabel.setFont(titleLabel.getFont().deriveFont(vip ? Font.BOLD : Font.PLAIN));
        group.add(titleLabel, BorderLayout.NORTH);
        int rows = (int) Math.ceil(stations.length / 4.0);
        JPanel grid = new JPanel(new GridLayout(rows, 4, 12, 12));
        grid.setPreferredSize(new Dimension(0, rows * 156 + (rows - 1) * 12));
        grid.setOpaque(false);
        for (Station station : stations) grid.add(stationCard(station));
        group.add(grid, BorderLayout.CENTER);
        return group;
    }

    
    private JPanel buildSelectionBar() {
        
        JPanel bar = card();
        bar.setLayout(new BorderLayout(12, 0));
        selectionLabel = label("Selected: none", 12, TEXT);
        bar.add(selectionLabel, BorderLayout.WEST);
        JPanel actions = row(8);
        JButton primary = button("Assign Next Customer", BLUE, Color.WHITE, true);
        primary.addActionListener(e -> assignToSelected());
        JButton close = button("\u2715", PANEL_DARK, MUTED, false);
        close.addActionListener(e -> clearSelection());
        actions.add(primary);
        actions.add(close);
        bar.add(actions, BorderLayout.EAST);
        bar.setVisible(false);
        return bar;
    }

    private void selectStation(StationSlot slot) {
        
        selectedStation = slot;
        selectionLabel.setText("Selected: " + slot.station.id());
        selectionBar.setVisible(true);
        footer.setText(slot.station.id() + " selected");
    }

    private void clearSelection() {
        selectedStation = null;
        if (selectionBar != null) selectionBar.setVisible(false);
    }

    private void clearSelectionIfSelected(StationSlot slot) {
        if (selectedStation == slot) clearSelection();
    }

    private void assignToSelected() {
        
        if (selectedStation == null) return;
        if (selectedStation.customer != null) {
            footer.setText("Selected terminal is already in use");
            return;
        }
        if (!"Available".equals(selectedStation.station.status())) {
            footer.setText(selectedStation.station.id() + " is not online");
            return;
        }
        if (waitingQueue.isEmpty()) {
            footer.setText("Queue is empty");
            return;
        }
        QueueModel.Customer customer = waitingQueue.peek();
        if (!selectedStation.matches(customer)) {
            footer.setText("Next customer needs a " + customer.type() + " terminal");
            return;
        }
        waitingQueue.dequeue();
        waitingCustomerIndex.remove(customer.name());
        StationSlot station = selectedStation;
        station.assign(customer);
        allocationHistory.push(new Allocation(customer, station));
        updateReservationStatus(reservationIndex.get(customer.name()), "assigned");
        servedCount++;
        peakLoad = Math.max(peakLoad, activeStationCount());
        log(customer.name() + " manually allocated to " + station.station.id());
        refreshWaiting();
        footer.setText(customer.name() + " allocated to " + station.station.id());
        clearSelection();
    }

    private JPanel stationCard(Station station) {
        
        JPanel card = card();
        card.setPreferredSize(new Dimension(196, 156));
        card.setMinimumSize(new Dimension(196, 156));
        card.setMaximumSize(new Dimension(196, 156));
        card.setLayout(new BorderLayout(10, 10));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label(station.id(), 11, MUTED), BorderLayout.WEST);
        StatusBadge statusLabel = status(station.status());
        top.add(statusLabel, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel detailLabel = label(station.detail(), 11, station.status().equals("Maintenance") ? AMBER : MUTED);
        JLabel extraLabel = label(station.extra(), 11, MUTED);
        info.add(detailLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(extraLabel);
        StationSlot slot = new StationSlot(station, card, statusLabel, detailLabel, extraLabel);
        availableStations.add(slot);
        terminalIndex.put(station.id(), slot);
        stationNetwork.addVertex(station.id());
        stationNetwork.addEdge("CONTROL", station.id(), 1);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) showStationMenu(slot);
                else if (slot.customer == null && "Available".equals(station.status())) onNodeClicked(station.id());
            }
        });
        card.add(info, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildRightRail() {
        
        JPanel rail = panel(BACKGROUND);
        rail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.add(buildNodeMap());
        rail.add(Box.createVerticalStrut(12));
        rail.add(buildSystem());
        rail.add(Box.createVerticalStrut(12));
        rail.add(buildToday());
        JScrollPane scroll = new JScrollPane(rail);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildNodeMap() {
        
        JPanel box = card();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(label("NODE MAP", 11, MUTED));
        box.add(Box.createVerticalStrut(12));
        JPanel map = new JPanel(new GridLayout(3, 4, 7, 7));
        map.setOpaque(false);
        String[] ids = {
                "VIP-01", "VIP-02", "VIP-03", "VIP-04",
                "STD-01", "STD-02", "STD-03", "STD-04",
                "STD-05", "STD-06", "STD-07", "STD-08"
        };
        for (String id : ids) {
            JPanel node = new JPanel();
            Color color = nodeColor(id);
            node.setBackground(color);
            node.setBorder(BorderFactory.createLineBorder(color.brighter()));
            node.setToolTipText(id);
            node.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            node.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    onNodeClicked(id);
                }
            });
            map.add(node);
        }
        box.add(map);
        box.add(Box.createVerticalStrut(10));
        JPanel labels = row(0);
        labels.add(label("VIP 1-4", 10, MUTED));
        labels.add(Box.createHorizontalGlue());
        labels.add(label("STD 5-12", 10, MUTED));
        box.add(labels);
        return box;
    }

    private Color nodeColor(String id) {
        StationSlot slot = terminalIndex.get(id);
        if (slot != null) {
            if (slot.customer != null) return new Color(75, 35, 45);
            if ("Maintenance".equals(slot.station.status())) return new Color(68, 52, 29);
            return new Color(30, 66, 59);
        }
        for (int i = 0; i < stationCatalog.size(); i++) {
            Station station = stationCatalog.get(i);
            if (station.id().equals(id) && "Maintenance".equals(station.status())) return new Color(68, 52, 29);
        }
        return new Color(75, 35, 45);
    }

    
    private void onNodeClicked(String id) {
        
        StationSlot slot = terminalIndex.get(id);
        if (slot == null) {
            footer.setText(id + " is not selectable right now");
            return;
        }
        if (slot.customer != null) {
            footer.setText(id + " is currently in use by " + slot.customer.name());
            return;
        }
        selectStation(slot);
        QueueModel.Graph.Path<String> route = stationNetwork.shortestPath("CONTROL", id);
        QueueModel.DynamicArray<String> breadthFirst = stationNetwork.breadthFirstSearch("CONTROL");
        QueueModel.DynamicArray<String> depthFirst = stationNetwork.depthFirstSearch("CONTROL");
        QueueModel.DynamicArray<String> backtracked = stationNetwork.findPathBacktracking("CONTROL", id);
        if (!route.isReachable() || backtracked.isEmpty()) {
            footer.setText(id + " selected · network route unavailable");
            return;
        }
        footer.setText(id + " selected · distance " + route.distance()
                + " · reachable " + breadthFirst.size()
                + " · traversed " + depthFirst.size()
                + " · path " + backtracked.size() + " nodes");
    }

    private void showStationMenu(StationSlot source) {
        Object[] options = source.customer == null
                ? new Object[]{"Set Online", "Set Maintenance", "Close"}
                : new Object[]{"Remove to Queue", "Transfer", "Close"};
        int choice = JOptionPane.showOptionDialog(frame,
                source.station.id() + "  " + source.station.status(),
                "Station controls", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (choice < 0 || "Close".equals(options[choice])) return;
        if ("Set Online".equals(options[choice])) {
            setStationStatus(source, "Available", "No active session", "");
        } else if ("Set Maintenance".equals(options[choice])) {
            String[] reasons = {"Hardware repair", "Software update", "Network issue", "Cleaning", "Other"};
            String reason = (String) JOptionPane.showInputDialog(frame, "Maintenance reason:",
                    "Station maintenance", JOptionPane.PLAIN_MESSAGE, null, reasons, reasons[0]);
            if (reason != null) setStationStatus(source, "Maintenance", reason, "Operator maintenance");
        } else if ("Remove to Queue".equals(options[choice])) {
            removeStationCustomer(source);
        } else if ("Transfer".equals(options[choice])) {
            transferStationCustomer(source);
        }
    }

    private void setStationStatus(StationSlot slot, String status, String detail, String extra) {
        if (slot.customer != null) {
            footer.setText("Remove the active customer before changing station status");
            return;
        }
        slot.station.setStatus(status, detail, extra);
        slot.statusLabel.update(status, "Maintenance".equals(status) ? AMBER : GREEN);
        slot.detailLabel.setText(detail);
        slot.detailLabel.setForeground("Maintenance".equals(status) ? AMBER : MUTED);
        slot.extraLabel.setText(extra);
        slot.card.revalidate();
        slot.card.repaint();
        refreshDashboard();
        footer.setText(slot.station.id() + " set to " + status);
    }

    private void removeStationCustomer(StationSlot station) {
        QueueModel.Customer customer = station.removeCustomer();
        if (customer == null) return;
        waitingQueue.enqueue(customer);
        waitingCustomerIndex.insert(customer.name());
        updateReservationStatus(reservationIndex.get(customer.name()), "queued");
        log(customer.name() + " removed from " + station.station.id() + " and returned to queue");
        refreshWaiting();
        footer.setText(customer.name() + " returned to the queue");
    }

    private void transferStationCustomer(StationSlot source) {
        QueueModel.DynamicArray<StationSlot> targets = new QueueModel.DynamicArray<>();
        for (int i = 0; i < availableStations.size(); i++) {
            StationSlot candidate = availableStations.get(i);
            if (candidate != source && candidate.customer == null
                    && "Available".equals(candidate.station.status())
                    && candidate.matches(source.customer)) targets.add(candidate);
        }
        if (targets.isEmpty()) {
            footer.setText("No compatible station is available for transfer");
            return;
        }
        String[] names = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) names[i] = targets.get(i).station.id();
        String selected = (String) JOptionPane.showInputDialog(frame, "Transfer remaining session time to:",
                "Transfer customer", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (selected == null) return;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).station.id().equals(selected)) {
                QueueModel.Customer customer = source.customer;
                int remaining = source.remainingSeconds();
                source.removeCustomer();
                targets.get(i).assign(customer, remaining);
                log(customer.name() + " transferred to " + selected);
                refreshDashboard();
                footer.setText(customer.name() + " transferred to " + selected);
                return;
            }
        }
    }

    private JPanel buildSystem() {
        
        JPanel box = card();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(label("SYSTEM", 11, MUTED));
        box.add(Box.createVerticalStrut(12));
        box.add(progress("CPU", "Measuring...", BLUE, 1));
        box.add(progress("Memory", "Measuring...", Color.LIGHT_GRAY, 1));
        box.add(progress("Network", "Checking...", GREEN, 1));
        refreshSystemStats();
        return box;
    }

    private JPanel buildToday() {
        
        JPanel box = card();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(label("TODAY", 11, MUTED));
        box.add(Box.createVerticalStrut(12));
        box.add(statLine("Served", "0"));
        box.add(statLine("Avg. wait", "0 min"));
        box.add(statLine("Peak load", "0"));
        box.add(statLine("Throughput", "0 / hr"));
        return box;
    }

    private JPanel buildFooter() {
        
        JPanel footerPanel = panel(BACKGROUND);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        footerPanel.setLayout(new BorderLayout());
        JPanel status = row(8);
        status.add(label("\u25cf", 11, GREEN));
        status.add(footer);
        footer.setFont(new Font("Monospaced", Font.PLAIN, 11));
        footer.setForeground(MUTED);
        footerPanel.add(status, BorderLayout.WEST);
        JLabel logsLink = label("Logs", 11, MUTED);
        logsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logsLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                showLogDialog();
            }

            @Override public void mouseEntered(MouseEvent event) {
                logsLink.setForeground(TEXT);
            }

            @Override public void mouseExited(MouseEvent event) {
                logsLink.setForeground(MUTED);
            }
        });
        JButton reservations = button("Reservations", PANEL_DARK, MUTED, false);
        reservations.addActionListener(event -> showReservationsDialog());
        JPanel footerActions = row(12);
        footerActions.add(reservations);
        footerActions.add(logsLink);
        footerPanel.add(footerActions, BorderLayout.EAST);
        return footerPanel;
    }

    
    private void showLogDialog() {
        
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(PANEL_DARK);
        area.setForeground(TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        if (activityLog.isEmpty()) {
            area.setText("No activity logged yet.");
        } else {
            for (int i = 0; i < activityLog.size(); i++) area.append(activityLog.get(i) + "\n");
        }
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(420, 320));
        JOptionPane.showMessageDialog(frame, scroll, "Admin Log", JOptionPane.PLAIN_MESSAGE);
    }

    private void showReservationsDialog() {
        JPanel reservationsPanel = new JPanel();
        reservationsPanel.setLayout(new BoxLayout(reservationsPanel, BoxLayout.Y_AXIS));
        reservationsPanel.setBackground(PANEL_DARK);
        reservationsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reservationsPanel.add(label("Loading reservations...", 12, MUTED));
        JScrollPane scroll = new JScrollPane(reservationsPanel);
        scroll.setPreferredSize(new Dimension(850, 420));
        JDialog dialog = new JDialog(frame, "Website Reservations", false);
        dialog.add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        loadReservations(reservationsPanel, dialog);
    }

    private void loadReservations(JPanel reservationsPanel, JDialog dialog) {
        if (WEB_API_TOKEN == null || WEB_API_TOKEN.isBlank()) {
            showReservationMessage(reservationsPanel, "Reservations unavailable. NETQUEUE_PUSH_TOKEN is missing.");
            return;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEB_API_URL + "/api/bookings/java"))
                .header("Authorization", "Bearer " + WEB_API_TOKEN)
                .header("Accept", "application/json")
                .GET()
                .build();
        WEB_API_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.statusCode() != 200) {
                        showReservationMessage(reservationsPanel,
                                "Reservations unavailable. Server returned HTTP " + response.statusCode() + ".");
                        return;
                    }
                    renderReservations(reservationsPanel, parseReservations(response.body()), dialog);
                }))
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() -> showReservationMessage(
                            reservationsPanel, "Reservations unavailable. Check the network connection."));
                    return null;
                });
    }

    private void showReservationMessage(JPanel panel, String message) {
        panel.removeAll();
        panel.add(label(message, 12, MUTED));
        panel.revalidate();
        panel.repaint();
    }

    private QueueModel.DynamicArray<Reservation> parseReservations(String response) {
        QueueModel.DynamicArray<Reservation> reservations = new QueueModel.DynamicArray<>();
        Matcher objects = Pattern.compile("\\{(.*?)\\}").matcher(response);
        while (objects.find()) {
            String object = objects.group(1);
            String id = jsonField(object, "id");
            if (id.isEmpty()) id = jsonField(object, "bookingId");
            String displayName = jsonField(object, "displayName");
            String email = jsonField(object, "email");
            String zone = jsonField(object, "zone");
            String start = jsonField(object, "startAt").replace('T', ' ');
            String end = jsonField(object, "endAt").replace('T', ' ');
            String status = jsonField(object, "status");
            if ((displayName.isEmpty() && email.isEmpty()) || id.isEmpty()) continue;
            reservations.add(new Reservation(id, displayName, email, zone, start, end, status));
        }
        return reservations;
    }

    private void renderReservations(JPanel panel, QueueModel.DynamicArray<Reservation> reservations,
                                    JDialog dialog) {
        panel.removeAll();
        if (reservations.isEmpty()) {
            panel.add(label("No upcoming reservations.", 12, MUTED));
        } else {
            for (int i = 0; i < reservations.size(); i++) {
                panel.add(reservationRow(reservations.get(i), panel, dialog));
                if (i < reservations.size() - 1) panel.add(Box.createVerticalStrut(8));
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private JPanel reservationRow(Reservation reservation, JPanel parent, JDialog dialog) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(PANEL);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(label(reservation.displayName + "  <" + reservation.email + ">", 12, TEXT));
        details.add(Box.createVerticalStrut(4));
        details.add(label(reservation.zone + "  " + reservation.start + " - " + reservation.end
                + "  [" + reservation.status + "]", 11, MUTED));
        row.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton queue = button("Add to Queue", BLUE, Color.WHITE, true);
        queue.addActionListener(event -> addReservationToQueue(reservation, queue));
        JButton assign = button("Assign Now", GREEN, Color.WHITE, true);
        assign.setEnabled(isScheduledNow(reservation));
        assign.setToolTipText(assign.isEnabled()
                ? "Assign this reservation to an available compatible PC"
                : "Enabled only during the reservation schedule");
        assign.addActionListener(event -> assignReservation(reservation, assign));
        JButton remove = button("Remove", PANEL_DARK, RED, false);
        remove.addActionListener(event -> removeReservation(reservation, remove, parent, dialog));
        actions.add(queue);
        actions.add(assign);
        actions.add(remove);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void addReservationToQueue(Reservation reservation, JButton button) {
        String name = reservation.displayName.trim();
        if (name.isEmpty()) {
            footer.setText("Reservation has no customer name");
            return;
        }
        if (waitingCustomerIndex.contains(name)) {
            footer.setText(name + " is already in the queue");
            return;
        }
        String type = "VIP".equalsIgnoreCase(reservation.zone) ? "VIP" : "Standard";
        QueueModel.Customer customer = new QueueModel.Customer(name, type, reservationDurationSeconds(reservation));
        waitingQueue.enqueue(customer);
        waitingCustomerIndex.insert(name);
        reservationIndex.put(name, reservation.id);
        updateReservationStatus(reservation.id, "queued");
        log(name + " from reservation joined the queue");
        refreshWaiting();
        button.setEnabled(false);
        button.setText("In Queue");
        footer.setText(name + " added to the queue");
    }

    private void assignReservation(Reservation reservation, JButton button) {
        if (!isScheduledNow(reservation)) {
            footer.setText("This reservation is not currently scheduled");
            button.setEnabled(false);
            return;
        }
        String name = reservation.displayName.trim();
        String type = "VIP".equalsIgnoreCase(reservation.zone) ? "VIP" : "Standard";
        QueueModel.Customer customer = new QueueModel.Customer(name, type, reservationDurationSeconds(reservation));
        StationSlot station = findAvailableStation(customer);
        if (station == null) {
            footer.setText("No available " + type + " station for " + name);
            return;
        }
        if (waitingCustomerIndex.contains(name)) removeWaitingCustomer(name);
        reservationIndex.put(name, reservation.id);
        station.assign(customer);
        updateReservationStatus(reservation.id, "assigned");
        allocationHistory.push(new Allocation(customer, station));
        servedCount++;
        peakLoad = Math.max(peakLoad, activeStationCount() + 1);
        log(name + " reservation assigned to " + station.station.id());
        refreshWaiting();
        button.setEnabled(false);
        footer.setText(name + " assigned to " + station.station.id());
    }

    private void removeWaitingCustomer(String name) {
        QueueModel.DynamicArray<QueueModel.Customer> remaining = waitingQueue.snapshot();
        while (!waitingQueue.isEmpty()) waitingQueue.dequeue();
        for (int i = 0; i < remaining.size(); i++) {
            QueueModel.Customer customer = remaining.get(i);
            if (!customer.name().equals(name)) waitingQueue.enqueue(customer);
        }
        waitingCustomerIndex.remove(name);
    }

    private void removeReservation(Reservation reservation, JButton remove,
                                   JPanel parent, JDialog dialog) {
        remove.setEnabled(false);
        remove.setText("Removing...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEB_API_URL + "/api/bookings/" + encodePath(reservation.id)))
                .header("Authorization", "Bearer " + WEB_API_TOKEN)
                .header("Accept", "application/json")
                .DELETE()
                .build();
        WEB_API_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        remove.setEnabled(true);
                        remove.setText("Remove");
                        footer.setText("Website did not remove reservation (HTTP "
                                + response.statusCode() + "): " + response.body());
                        return;
                    }
                    
                    loadReservations(parent, dialog);
                    footer.setText(reservation.displayName + "'s reservation was removed from the website");
                }))
                .exceptionally(error -> {
                    SwingUtilities.invokeLater(() -> {
                        remove.setEnabled(true);
                        remove.setText("Remove");
                        footer.setText("Could not remove reservation: " + error.getMessage());
                    });
                    return null;
                });
    }

    private static String encodePath(String value) {
        return value.replace("%", "%25").replace(" ", "%20").replace("/", "%2F");
    }

    private boolean isScheduledNow(Reservation reservation) {
        try {
            LocalDateTime start = parseReservationTime(reservation.start);
            LocalDateTime end = parseReservationTime(reservation.end);
            LocalDateTime now = LocalDateTime.now();
            return !now.isBefore(start) && now.isBefore(end);
        } catch (DateTimeParseException error) {
            return false;
        }
    }

    private int reservationDurationSeconds(Reservation reservation) {
        try {
            long seconds = java.time.Duration.between(parseReservationTime(reservation.start),
                    parseReservationTime(reservation.end)).getSeconds();
            return (int) Math.max(60, Math.min(24 * 60 * 60, seconds));
        } catch (DateTimeParseException error) {
            return 60 * 60;
        }
    }

    private LocalDateTime parseReservationTime(String value) {
        String normalized = value.trim().replace(' ', 'T');
        try {
            return OffsetDateTime.parse(normalized).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(normalized);
        }
    }

    private static String jsonField(String object, String key) {
        Matcher field = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\":\\\"([^\\\"]*)\\\"").matcher(object);
        if (field.find()) return field.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        Matcher numericField = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\":(-?\\d+)").matcher(object);
        return numericField.find() ? numericField.group(1) : "";
    }

    private void updateReservationStatus(String bookingId, String status) {
        if (bookingId == null || bookingId.isBlank() || WEB_API_TOKEN == null || WEB_API_TOKEN.isBlank()) return;
        String body = "{\"status\":\"" + jsonEscape(status) + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEB_API_URL + "/api/bookings/" + encodePath(bookingId) + "/status"))
                .header("Authorization", "Bearer " + WEB_API_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        WEB_API_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        System.err.println("Booking status update rejected: HTTP " + response.statusCode());
                    }
                })
                .exceptionally(error -> {
                    System.err.println("Booking status update failed: " + error.getMessage());
                    return null;
                });
    }

    private void allocateNext(ActionEvent event) {
        
        if (waitingQueue.isEmpty()) {
            footer.setText("Queue is empty");
            return;
        }
        QueueModel.Customer customer = waitingQueue.peek();
        StationSlot station = findAvailableStation(customer);
        if (station == null) {
            footer.setText("No available " + customer.type() + " station");
            return;
        }
        waitingQueue.dequeue();
        waitingCustomerIndex.remove(customer.name());
        station.assign(customer);
        allocationHistory.push(new Allocation(customer, station));
        servedCount++;
        peakLoad = Math.max(peakLoad, activeStationCount() + 1);
        log(customer.name() + " allocated to " + station.station.id());
        refreshWaiting();
        footer.setText(customer.name() + " allocated to " + station.station.id());
    }

    private void undoAllocation() {
        
        if (allocationHistory.isEmpty()) {
            footer.setText("Nothing to undo");
            return;
        }
        Allocation allocation = allocationHistory.pop();
        allocation.station.clear();
        waitingQueue.enqueue(allocation.customer);
        waitingCustomerIndex.insert(allocation.customer.name());
        updateReservationStatus(reservationIndex.get(allocation.customer.name()), "queued");
        log("Undo: " + allocation.customer.name() + " returned to queue");
        refreshWaiting();
        footer.setText(allocation.customer.name() + " returned to the queue");
    }

    





    private StationSlot findAvailableStation(QueueModel.Customer customer) {
        for (int i = 0; i < availableStations.size(); i++) {
            StationSlot station = availableStations.get(i);
            if (station.customer == null && "Available".equals(station.station.status()) && station.matches(customer)) return station;
        }
        return null;
    }

    private void addStations(Station[] stations) {
        
        for (Station station : stations) stationCatalog.add(station);
    }

    
    private void log(String message) {
        String entry = LocalTime.now().format(TIME_FORMAT) + "  " + message;
        activityLog.add(entry);
    }

    private void refreshDashboard() {
        
        refreshSystemStats();
        int open = 0;
        int inUse = 0;
        int offline = 0;
        for (int i = 0; i < stationCatalog.size(); i++) {
            Station station = stationCatalog.get(i);
            StationSlot slot = slotFor(station);
            if (slot != null && slot.customer != null) inUse++;
            else if ("Available".equals(station.status())) open++;
            else if ("In Use".equals(station.status())) inUse++;
            else if ("Maintenance".equals(station.status())) offline++;
        }
        int total = stationCatalog.size();
        int averageWait = waitingQueue.size() * 3;
        if (averageWaitValue != null) averageWaitValue.setText(averageWait + " min");
        if (activeValue != null) activeValue.setText(inUse + "/" + total);
        if (stationSummary != null) stationSummary.setText(total + " stations · live");
        if (openSummary != null) openSummary.setText(String.valueOf(open));
        if (inUseSummary != null) inUseSummary.setText(String.valueOf(inUse));
        if (offlineSummary != null) offlineSummary.setText(String.valueOf(offline));
        if (servedValue != null) servedValue.setText(String.valueOf(servedCount));
        if (todayWaitValue != null) todayWaitValue.setText(averageWait + " min");
        if (peakValue != null) peakValue.setText(String.valueOf(peakLoad));
        if (throughputValue != null) throughputValue.setText(servedCount + " / hr");
        pushSnapshot();
    }

    private void pushSnapshot() {
        if (WEB_API_TOKEN == null || WEB_API_TOKEN.isBlank()) {
            if (footer != null) footer.setText("Web sync disabled: NETQUEUE_PUSH_TOKEN is missing");
            return;
        }

        StringBuilder json = new StringBuilder("{\"stations\":[");
        for (int i = 0; i < stationCatalog.size(); i++) {
            if (i > 0) json.append(',');
            Station station = stationCatalog.get(i);
            StationSlot slot = slotFor(station);
            String status = station.status();
            String detail = station.detail();
            if (slot != null && slot.customer != null) {
                status = "In Use";
                detail = "Session in progress";
            }
            json.append("{\"id\":\"").append(jsonEscape(station.id()))
                    .append("\",\"zone\":\"").append(station.id().startsWith("VIP-") ? "VIP" : "Standard")
                    .append("\",\"status\":\"").append(jsonEscape(status))
                    .append("\",\"detail\":\"").append(jsonEscape(detail)).append("\"}");
        }
        json.append("],\"queueLength\":").append(waitingQueue.size()).append('}');

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEB_API_URL + "/api/stations/update"))
                .header("Authorization", "Bearer " + WEB_API_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();
        WEB_API_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() != 204) {
                        System.err.println("NetQueue web sync rejected: HTTP " + response.statusCode());
                    }
                })
                .exceptionally(error -> {
                    System.err.println("NetQueue web sync failed: " + error.getMessage());
                    return null;
                });
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private int activeStationCount() {
        
        int active = 0;
        for (int i = 0; i < stationCatalog.size(); i++) {
            Station station = stationCatalog.get(i);
            StationSlot slot = slotFor(station);
            if ((slot != null && slot.customer != null) || "In Use".equals(station.status())) active++;
        }
        return active;
    }

    




    private StationSlot slotFor(Station station) {
        return terminalIndex.get(station.id());
    }

    private JPanel progress(String name, String value, Color color, int percent) {
        
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(statLine(name, value), BorderLayout.NORTH);
        JPanel track = new JPanel(new BorderLayout());
        track.setPreferredSize(new Dimension(0, 3));
        track.setBackground(new Color(66, 67, 72));
        JPanel fill = new JPanel();
        fill.setBackground(color);
        fill.setPreferredSize(new Dimension(percent, 3));
        track.add(fill, BorderLayout.WEST);
        panel.add(track, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 0));
        return panel;
    }

    private void refreshSystemStats() {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpu = os.getCpuLoad();
        int cpuPercent = cpu >= 0 ? (int) Math.round(cpu * 100) : 0;
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        int memoryPercent = (int) Math.round(used * 100.0 / runtime.maxMemory());
        int up = 0;
        int total = 0;
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isLoopback()) {
                    total++;
                    if (network.isUp()) up++;
                }
            }
        } catch (SocketException ignored) {
            total = 0;
        }
        if (cpuValue != null) cpuValue.setText(cpuPercent + "%");
        if (memoryValue != null) memoryValue.setText(memoryPercent + "%");
        if (networkValue != null) networkValue.setText(up + "/" + total + " up");
    }

    private JPanel statLine(String name, String value) {
        
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.add(label(name, 12, MUTED), BorderLayout.WEST);
        JLabel valueLabel = label(value, 12, TEXT);
        if ("Served".equals(name)) servedValue = valueLabel;
        if ("Avg. wait".equals(name)) todayWaitValue = valueLabel;
        if ("Peak load".equals(name)) peakValue = valueLabel;
        if ("Throughput".equals(name)) throughputValue = valueLabel;
        if ("CPU".equals(name)) cpuValue = valueLabel;
        if ("Memory".equals(name)) memoryValue = valueLabel;
        if ("Network".equals(name)) networkValue = valueLabel;
        line.add(valueLabel, BorderLayout.EAST);
        return line;
    }

    private JPanel legend() {
        
        JPanel legend = row(14);
        legend.add(status("Available"));
        legend.add(status("In Use"));
        legend.add(status("Maintenance"));
        return legend;
    }

    private StatusBadge status(String value) {
        
        Color color = value.equals("Available") ? GREEN : value.equals("In Use") ? RED : AMBER;
        return new StatusBadge(value, color);
    }

    
    




    private static final class StatusBadge extends JPanel {
        private Color accent;
        private final JLabel textLabel;

        private StatusBadge(String text, Color accent) {
            
            super(new FlowLayout(FlowLayout.CENTER, 0, 0));
            this.accent = accent;
            setOpaque(false);
            setPreferredSize(new Dimension(88, 28));
            setMinimumSize(new Dimension(88, 28));
            setMaximumSize(new Dimension(88, 28));
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            textLabel = new CrispLabel(text);
            textLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            textLabel.setForeground(accent);
            textLabel.setHorizontalAlignment(JLabel.CENTER);
            add(textLabel);
        }

        private void update(String text, Color accent) {
            
            this.accent = accent;
            textLabel.setText(text);
            textLabel.setForeground(accent);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private JPanel summaryValue(String value, String name, Color color) {
        
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel number = label(value, 21, color);
        number.setAlignmentX(Component.CENTER_ALIGNMENT);
        if ("open".equals(name)) openSummary = number;
        if ("in use".equals(name)) inUseSummary = number;
        if ("offline".equals(name)) offlineSummary = number;
        JLabel caption = label(name, 10, MUTED);
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(number);
        panel.add(caption);
        return panel;
    }

    private JPanel panel(Color color) {
        
        JPanel panel = new JPanel();
        panel.setBackground(color);
        return panel;
    }

    private JPanel card() {
        
        JPanel panel = panel(PANEL);
        panel.putClientProperty("FlatLaf.style", "arc: 12; borderWidth: 1; borderColor: " + hex(BORDER));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private static String hex(Color color){
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private JPanel row(int gap) {
        
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        panel.setOpaque(false);
        return panel;
    }

    private JLabel label(String text, int size, Color color) {
        
        JLabel label = new CrispLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private static final class CrispLabel extends JLabel {
        private CrispLabel(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private JButton button(String text, Color background, Color foreground, boolean filled) {
        
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        String style = filled ? "arc: 8" : "arc: 8; borderWidth: 1; borderColor: " + hex(BORDER);
        button.putClientProperty("FlatLaf.style", style);
        addHoverEffect(button, background, filled ? background.brighter() : PANEL.brighter());
        return button;
    }

    
    private void addHoverEffect(JButton button, Color normal, Color hover) {
        
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                button.setBackground(hover);
            }

            @Override public void mouseExited(MouseEvent event) {
                button.setBackground(normal);
            }
        });
    }

    private void styleField(JTextField field, String placeholder) {
        
        field.setText(placeholder);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(MUTED);
        field.setBackground(new Color(20, 21, 24));
        field.setCaretColor(TEXT);
        field.putClientProperty("FlatLaf.style",
                "arc: 8; borderWidth: 1; borderColor: " + hex(BORDER) + "; focusedBorderColor: " + hex(BLUE));
        field.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent event) {
                if (field.getText().equals(placeholder)) field.setText("");
                field.setForeground(TEXT);
            }
            @Override public void focusLost(java.awt.event.FocusEvent event) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(MUTED);
                }
            }
        });
    }

    private JPanel separator() {
        
        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(1, 22));
        separator.setBackground(BORDER);
        return separator;
    }

    private static final class Station {
        private final String id;
        private String status;
        private String detail;
        private String extra;

        private Station(String id, String status, String detail, String extra) {
            this.id = id;
            this.status = status;
            this.detail = detail;
            this.extra = extra;
        }

        Station(String id, String status, String detail) {
            this(id, status, detail, "");
        }

        String id() { return id; }
        String status() { return status; }
        String detail() { return detail; }
        String extra() { return extra; }

        void setStatus(String status, String detail, String extra) {
            this.status = status;
            this.detail = detail;
            this.extra = extra;
        }
    }

    
    private record Allocation(QueueModel.Customer customer, StationSlot station) {
    }

    private record Reservation(String id, String displayName, String email, String zone,
                               String start, String end, String status) {
    }

    




    private final class StationSlot {
        
        private final Station station;
        private final JPanel card;
        private final StatusBadge statusLabel;
        private final JLabel detailLabel;
        private final JLabel extraLabel;
        private QueueModel.Customer customer;
        private Timer sessionTimer;
        private int remainingSeconds;

        private StationSlot(Station station, JPanel card, StatusBadge statusLabel,
                            JLabel detailLabel, JLabel extraLabel) {
            
            this.station = station;
            this.card = card;
            this.statusLabel = statusLabel;
            this.detailLabel = detailLabel;
            this.extraLabel = extraLabel;
        }

        private void assign(QueueModel.Customer customer) {
            assign(customer, customer.durationSeconds());
        }

        private void assign(QueueModel.Customer customer, int durationSeconds) {
            
            this.customer = customer;
            station.setStatus("In Use", customer.name(), "Session in progress");
            statusLabel.update("In Use", RED);
            detailLabel.setText(customer.name());
            detailLabel.setForeground(TEXT);
            card.setBackground(new Color(48, 37, 41));
            card.revalidate();
            card.repaint();
            startSessionTimer(durationSeconds);
            clearSelectionIfSelected(this);
        }

        private boolean matches(QueueModel.Customer customer) {
            
            String stationType = station.id().startsWith("VIP-") ? "VIP" : "Standard";
            return stationType.equals(customer.type());
        }

        
        private void startSessionTimer(int durationSeconds) {
            
            remainingSeconds = Math.max(60, durationSeconds);
            updateCountdownLabel();
            if (sessionTimer != null) sessionTimer.stop();
            sessionTimer = new Timer(1000, event -> tick());
            sessionTimer.start();
        }

        private void tick() {
            
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                String customerName = customer != null ? customer.name() : "Customer";
                String stationId = station.id();
                updateReservationStatus(reservationIndex.get(customerName), "completed");
                clear();
                log(customerName + "'s session on " + stationId + " ended automatically");
                refreshDashboard();
                footer.setText(customerName + "'s session on " + stationId + " completed");
                return;
            }
            updateCountdownLabel();
        }

        private void updateCountdownLabel() {
            
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            extraLabel.setText(String.format("%02d:%02d remaining", minutes, seconds));
        }

        private void clear() {
            
            if (sessionTimer != null) {
                sessionTimer.stop();
                sessionTimer = null;
            }
            customer = null;
            station.setStatus("Available", "No active session", "");
            statusLabel.update("Available", GREEN);
            detailLabel.setText("No active session");
            detailLabel.setForeground(MUTED);
            extraLabel.setText("");
            card.setBackground(PANEL);
            card.revalidate();
            card.repaint();
            clearSelectionIfSelected(this);
        }

        private int remainingSeconds() {
            return remainingSeconds;
        }

        private QueueModel.Customer removeCustomer() {
            if (customer == null) return null;
            if (sessionTimer != null) {
                sessionTimer.stop();
                sessionTimer = null;
            }
            QueueModel.Customer removed = customer;
            customer = null;
            station.setStatus("Available", "No active session", "");
            statusLabel.update("Available", GREEN);
            detailLabel.setText("No active session");
            detailLabel.setForeground(MUTED);
            extraLabel.setText("");
            card.setBackground(PANEL);
            card.revalidate();
            card.repaint();
            return removed;
        }
    }

    private JPanel metric(String value, String name) {
        
        JPanel metric = new JPanel();
        metric.setOpaque(false);
        metric.setLayout(new BoxLayout(metric, BoxLayout.Y_AXIS));
        JLabel top = label(value, 13, TEXT);
        top.setAlignmentX(Component.CENTER_ALIGNMENT);
        if ("avg wait".equals(name)) averageWaitValue = top;
        if ("active".equals(name)) activeValue = top;
        JLabel bottom = label(name, 10, MUTED);
        bottom.setAlignmentX(Component.CENTER_ALIGNMENT);
        metric.add(top);
        metric.add(bottom);
        return metric;
    }
}
