import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

/**
 * BusLens - Bus Passenger Management Dashboard (Java Swing)
 * Queue (ArrayDeque) : passengers waiting at the stop, FIFO boarding
 * ArrayList          : passengers on board, always sorted by name
 * HashMap            : Ticket ID -> Passenger (O(1) lookup)
 * Binary Search      : search on-board passengers by name / prefix (O(log n))
 *
 * Compile : javac BusLensDashboard.java
 * Run     : java BusLensDashboard
 */
public class BusLensDashboard extends JFrame {

    static final Color SIDE1 = new Color(0x2B1566), SIDE2 = new Color(0x4527A0);
    static final Color BG = new Color(0xF3EFFB), TEXT = new Color(0x241A45), MUTED = new Color(0x7A6F99);
    static final Color P = new Color(0x7C4DFF), PD = new Color(0x4527A0), PL = new Color(0xEDE7F6);
    static final Color LINE = new Color(0xE6DEF7), RED = new Color(0xE5384F), GREEN = new Color(0x2E9E5B);
    static final Color TERM = new Color(0x1A1033), TERM_T = new Color(0xD9CFF5);
    static final String[] STOPS = {"Central", "Airport", "Market", "University", "Station"};
    static final int[] FARES = {20, 60, 15, 25, 30};
    static final int CAPACITY = 10;

    // ================= MODEL =================
    static class Passenger {
        final String id, name, stop;
        final int fare;
        Passenger(String id, String name, String stop, int fare) {
            this.id = id; this.name = name.trim(); this.stop = stop; this.fare = fare;
        }
    }

    static class Bus {
        final Queue<Passenger> waiting = new ArrayDeque<>();
        final ArrayList<Passenger> onboard = new ArrayList<>();      // sorted by name
        final HashMap<String, Passenger> index = new HashMap<>();    // id -> passenger
        int nextTicket = 1001, lastSteps = 0;
        String trace = "";

        Passenger join(String name, String stop) {
            int f = 0;
            for (int i = 0; i < STOPS.length; i++) if (STOPS[i].equals(stop)) f = FARES[i];
            Passenger p = new Passenger("T" + nextTicket++, name, stop, f);
            index.put(p.id, p);
            waiting.offer(p);                       // enqueue at the back
            return p;
        }

        Passenger boardNext() {
            if (waiting.isEmpty() || onboard.size() >= CAPACITY) return null;
            Passenger p = waiting.poll();           // dequeue from the front
            onboard.add(lowerBound(p.name.toLowerCase()), p);
            return p;
        }

        boolean alight(String id) {
            Passenger p = index.get(id);
            if (p == null || !onboard.remove(p)) return false;
            index.remove(id);
            return true;
        }

        /** Binary search (lower bound): first index whose name >= key. */
        int lowerBound(String key) {
            int lo = 0, hi = onboard.size();
            lastSteps = 0;
            StringBuilder sb = new StringBuilder();
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                lastSteps++;
                String n = onboard.get(mid).name;
                boolean right = n.toLowerCase().compareTo(key) < 0;
                sb.append(String.format("step %d  low=%d high=%d mid=%d%n   \"%s\" %s \"%s\" -> go %s%n",
                        lastSteps, lo, hi, mid, n, right ? "<" : ">=", key, right ? "right" : "left"));
                if (right) lo = mid + 1; else hi = mid;
            }
            trace = sb.toString();
            return lo;
        }

        List<Passenger> search(String text) {
            String key = text.trim().toLowerCase();
            List<Passenger> r = new ArrayList<>();
            for (int i = lowerBound(key); i < onboard.size()
                    && onboard.get(i).name.toLowerCase().startsWith(key); i++) r.add(onboard.get(i));
            return r;
        }

        int fareTotal() { int t = 0; for (Passenger p : onboard) t += p.fare; return t; }
    }

    // ================= COMPONENTS =================
    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static class Card extends JPanel {
        final boolean hero;
        Card(LayoutManager l, boolean hero) {
            super(l); this.hero = hero; setOpaque(false); setBorder(new EmptyBorder(14, 18, 14, 18));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); smooth(g2);
            RoundRectangle2D s = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            if (hero) g2.setPaint(new GradientPaint(0, 0, P, getWidth(), getHeight(), PD)); else g2.setColor(Color.WHITE);
            g2.fill(s);
            if (!hero) { g2.setColor(LINE); g2.draw(s); }
            g2.dispose();
        }
    }

    static class Gradient extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, SIDE1, 0, getHeight(), SIDE2));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** Row of seats: filled = occupied. */
    static class Seats extends JComponent {
        int used = 0;
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); smooth(g2);
            int gap = 6, w = (getWidth() - gap * (CAPACITY - 1)) / CAPACITY;
            for (int i = 0; i < CAPACITY; i++) {
                g2.setColor(i < used ? P : PL);
                g2.fill(new RoundRectangle2D.Float(i * (w + gap), 2, w, 26, 10, 10));
            }
            g2.setColor(MUTED); g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString(used + " of " + CAPACITY + " seats taken", 0, 46);
            g2.dispose();
        }
    }

    // ================= APP =================
    private final Bus bus = new Bus();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Ticket", "Passenger", "Stop", "Fare"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final DefaultListModel<String> queueModel = new DefaultListModel<>();
    private final JTextField search = new JTextField(), nameField = new JTextField();
    private final JComboBox<String> stopBox = new JComboBox<>(STOPS);
    private final JLabel status = new JLabel(" ");
    private final JTextArea terminal = new JTextArea();
    private final JLabel kWait = big(Color.WHITE), kOn = big(TEXT), kLeft = big(TEXT), kFare = big(TEXT);
    private final JLabel sQ = side(), sL = side(), sSteps = side();
    private final Seats seats = new Seats();

    public BusLensDashboard() {
        super("BusLens - Bus Passenger Management");
        String[] seed = {"Meera Nair", "Arjun Rao", "Divya Shah", "Imran Khan", "Sara Thomas", "Kabir Singh", "Anita Das",
                "Rohit Verma", "Priya Menon", "Neil Fernandes", "Tara Iyer", "Vikram Joshi", "Zoya Ali", "Leela Pillai"};
        for (int i = 0; i < seed.length; i++) bus.join(seed[i], STOPS[i % STOPS.length]);
        for (int i = 0; i < 6; i++) bus.boardNext();

        setLayout(new BorderLayout());
        add(sidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(22, 24, 22, 24));
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(title(), BorderLayout.NORTH);
        top.add(kpis(), BorderLayout.CENTER);
        main.add(top, BorderLayout.NORTH);
        JPanel mid = new JPanel(new GridLayout(1, 2, 16, 0));
        mid.setOpaque(false);
        mid.add(passengerCard());
        mid.add(rightColumn());
        main.add(mid, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { runSearch(); }
            public void removeUpdate(DocumentEvent e) { runSearch(); }
            public void changedUpdate(DocumentEvent e) { runSearch(); }
        });
        refresh(bus.onboard);
        terminal.setText("$ binary-search --trace\nType a passenger name to watch every step.");
        say("Bus is ready. Passengers on board are kept sorted by name.", MUTED);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null);
    }

    // ---------- actions ----------
    private void runSearch() {
        String q = search.getText().trim();
        if (q.isEmpty()) { refresh(bus.onboard); terminal.setText("$ binary-search --trace\nType a passenger name to watch every step."); say("Showing all passengers on board", MUTED); return; }
        List<Passenger> r = bus.search(q);
        refresh(r);
        terminal.setText("$ binary-search \"" + q + "\"\n" + bus.trace + "found " + r.size() + " match(es) in " + bus.lastSteps + " steps");
        terminal.setCaretPosition(0);
        say(r.isEmpty() ? "No passenger on board starts with \"" + q + "\"" : r.size() + " passenger(s) found", r.isEmpty() ? RED : GREEN);
    }

    private void join() {
        String n = nameField.getText().trim();
        if (n.isEmpty()) { say("Enter a passenger name first", RED); return; }
        Passenger p = bus.join(n, (String) stopBox.getSelectedItem());
        nameField.setText("");
        refresh(bus.onboard);
        say(p.name + " joined the queue with ticket " + p.id, GREEN);
    }

    private void board() {
        if (bus.waiting.isEmpty()) { say("Nobody is waiting at the stop", RED); return; }
        if (bus.onboard.size() >= CAPACITY) { say("Bus is full - drop someone first", RED); return; }
        Passenger p = bus.boardNext();
        search.setText("");
        refresh(bus.onboard);
        say(p.name + " boarded (dequeued from front of queue)", GREEN);
    }

    private void drop() {
        int row = table.getSelectedRow();
        if (row < 0) { say("Select a passenger in the table first", RED); return; }
        String id = (String) model.getValueAt(row, 0);
        String nm = (String) model.getValueAt(row, 1);
        bus.alight(id);
        runSearch();
        say(nm + " got off the bus", MUTED);
    }

    private void refresh(List<Passenger> shown) {
        model.setRowCount(0);
        for (Passenger p : shown) model.addRow(new Object[]{p.id, p.name, p.stop, "Rs " + p.fare});
        queueModel.clear();
        int pos = 1;
        for (Passenger p : bus.waiting) queueModel.addElement((pos++) + ".  " + p.name + "   -> " + p.stop);
        kWait.setText(String.valueOf(bus.waiting.size()));
        kOn.setText(String.valueOf(bus.onboard.size()));
        kLeft.setText(String.valueOf(CAPACITY - bus.onboard.size()));
        kFare.setText("Rs " + bus.fareTotal());
        sQ.setText(String.valueOf(bus.waiting.size()));
        sL.setText(String.valueOf(bus.onboard.size()));
        sSteps.setText(String.valueOf(bus.lastSteps));
        seats.used = bus.onboard.size();
        seats.repaint();
    }

    private void say(String msg, Color c) { status.setText(msg); status.setForeground(c); }

    // ---------- UI builders ----------
    private static JLabel big(Color c) { JLabel l = new JLabel("0"); l.setFont(new Font("SansSerif", Font.BOLD, 26)); l.setForeground(c); return l; }
    private static JLabel side() { JLabel l = new JLabel("0", SwingConstants.RIGHT); l.setFont(new Font("SansSerif", Font.BOLD, 14)); l.setForeground(Color.WHITE); return l; }
    private static JLabel heading(String t) {
        JLabel l = new JLabel(t); l.setFont(new Font("SansSerif", Font.BOLD, 15)); l.setForeground(TEXT);
        l.setBorder(new EmptyBorder(0, 0, 8, 0)); return l;
    }
    private static JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13)); b.setFocusPainted(false); b.setBorderPainted(false);
        b.setOpaque(true); b.setBackground(bg); b.setForeground(fg);
        b.setBorder(new EmptyBorder(9, 16, 9, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel sidebar() {
        Gradient s = new Gradient();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setPreferredSize(new Dimension(220, 100));
        s.setBorder(new EmptyBorder(24, 18, 22, 18));
        JLabel logo = new JLabel("BusLens"); logo.setFont(new Font("SansSerif", Font.BOLD, 22)); logo.setForeground(Color.WHITE);
        JLabel tag = new JLabel("Passenger Management"); tag.setFont(new Font("SansSerif", Font.PLAIN, 12)); tag.setForeground(new Color(0xD1C4FF));
        s.add(logo); s.add(tag); s.add(Box.createVerticalGlue());
        JPanel live = new JPanel(new GridLayout(4, 2, 4, 8));
        live.setOpaque(false);
        JLabel h = new JLabel("LIVE STRUCTURES"); h.setFont(new Font("SansSerif", Font.BOLD, 11)); h.setForeground(new Color(0xCDBFFF));
        live.add(h); live.add(new JLabel());
        String[] names = {"Queue size", "ArrayList size", "Last bin. steps"};
        JLabel[] vals = {sQ, sL, sSteps};
        for (int i = 0; i < 3; i++) {
            JLabel n = new JLabel(names[i]); n.setFont(new Font("SansSerif", Font.PLAIN, 12)); n.setForeground(new Color(0xE4DCFF));
            live.add(n); live.add(vals[i]);
        }
        s.add(live);
        return s;
    }

    private JPanel title() {
        JPanel t = new JPanel(new BorderLayout());
        t.setOpaque(false);
        JLabel h = new JLabel("Bus Passenger Management");
        h.setFont(new Font("SansSerif", Font.BOLD, 24)); h.setForeground(TEXT);
        JLabel sub = new JLabel("Queue at the stop, sorted ArrayList on the bus, binary search by name");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13)); sub.setForeground(MUTED);
        JPanel box = new JPanel(new GridLayout(2, 1)); box.setOpaque(false); box.add(h); box.add(sub);
        t.add(box, BorderLayout.WEST);
        return t;
    }

    private JPanel kpis() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(100, 88));
        String[] caps = {"Waiting in queue", "On board", "Seats left", "Fare collected"};
        JLabel[] vals = {kWait, kOn, kLeft, kFare};
        for (int i = 0; i < 4; i++) {
            Card c = new Card(new BorderLayout(), i == 0);
            JLabel cap = new JLabel(caps[i]); cap.setFont(new Font("SansSerif", Font.BOLD, 12));
            cap.setForeground(i == 0 ? new Color(0xE0D4FF) : MUTED);
            c.add(cap, BorderLayout.NORTH); c.add(vals[i], BorderLayout.CENTER);
            row.add(c);
        }
        return row;
    }

    private JPanel passengerCard() {
        Card c = new Card(new BorderLayout(0, 8), false);
        c.add(heading("Passengers on board (ArrayList)"), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        search.setFont(new Font("SansSerif", Font.PLAIN, 14));
        search.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(8, 12, 8, 12)));
        JPanel searchRow = new JPanel(new BorderLayout(0, 4));
        searchRow.setOpaque(false);
        JLabel sl = new JLabel("Search by name (binary search)"); sl.setFont(new Font("SansSerif", Font.BOLD, 12)); sl.setForeground(MUTED);
        searchRow.add(sl, BorderLayout.NORTH); searchRow.add(search, BorderLayout.CENTER);
        body.add(searchRow, BorderLayout.NORTH);

        table.setRowHeight(30); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 13)); table.setForeground(TEXT);
        table.setSelectionBackground(PL); table.setSelectionForeground(PD);
        JTableHeader th = table.getTableHeader();
        th.setFont(new Font("SansSerif", Font.BOLD, 12)); th.setBackground(PL); th.setForeground(PD); th.setReorderingAllowed(false);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(LINE)); sp.getViewport().setBackground(Color.WHITE);
        body.add(sp, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout(0, 6)); foot.setOpaque(false);
        JButton drop = button("Drop selected", PL, PD);
        drop.addActionListener(e -> drop());
        status.setFont(new Font("SansSerif", Font.BOLD, 12));
        foot.add(drop, BorderLayout.WEST); foot.add(status, BorderLayout.SOUTH);
        body.add(foot, BorderLayout.SOUTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    private JPanel rightColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 16));
        col.setOpaque(false);

        Card q = new Card(new BorderLayout(0, 8), false);
        q.add(heading("Boarding queue (FIFO)"), BorderLayout.NORTH);
        JList<String> list = new JList<>(queueModel);
        list.setFont(new Font("SansSerif", Font.PLAIN, 13)); list.setForeground(TEXT); list.setFixedCellHeight(26);
        list.setEnabled(false);
        JScrollPane lsp = new JScrollPane(list); lsp.setBorder(new LineBorder(LINE));
        q.add(lsp, BorderLayout.CENTER);

        JPanel form = new JPanel(new BorderLayout(8, 8)); form.setOpaque(false);
        nameField.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(7, 10, 7, 10)));
        nameField.addActionListener(e -> join());
        JPanel inputs = new JPanel(new GridLayout(1, 2, 8, 0)); inputs.setOpaque(false);
        inputs.add(nameField); inputs.add(stopBox);
        JPanel btns = new JPanel(new GridLayout(1, 2, 8, 0)); btns.setOpaque(false);
        JButton j = button("Join queue", P, Color.WHITE), b = button("Board next", PD, Color.WHITE);
        j.addActionListener(e -> join()); b.addActionListener(e -> board());
        btns.add(j); btns.add(b);
        form.add(inputs, BorderLayout.NORTH); form.add(btns, BorderLayout.SOUTH);
        form.add(seats, BorderLayout.CENTER);
        seats.setPreferredSize(new Dimension(100, 56));
        q.add(form, BorderLayout.SOUTH);
        col.add(q, BorderLayout.CENTER);

        Card t = new Card(new BorderLayout(0, 6), false);
        t.setPreferredSize(new Dimension(100, 190));
        t.add(heading("Binary search trace"), BorderLayout.NORTH);
        terminal.setEditable(false); terminal.setBackground(TERM); terminal.setForeground(TERM_T);
        terminal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); terminal.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane tsp = new JScrollPane(terminal); tsp.setBorder(null);
        t.add(tsp, BorderLayout.CENTER);
        col.add(t, BorderLayout.SOUTH);
        return col;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BusLensDashboard().setVisible(true));
    }
}
