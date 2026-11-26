import java.util.*;

/**
 * Bus Passenger Management System
 *
 * Queue     -> waiting line at the bus stop (first come, first served)
 * ArrayList -> passengers currently on board (seated)
 * HashMap   -> ticket ID -> Passenger registry for fast lookup
 */
public class BusPassengerManagement {

    // ---------- Passenger model ----------
    static class Passenger {
        private final int ticketId;
        private final String name;
        private final String destination;
        private int seatNumber = -1; // -1 = not seated yet
        private String status = "WAITING";

        Passenger(int ticketId, String name, String destination) {
            this.ticketId = ticketId;
            this.name = name;
            this.destination = destination;
        }

        int getTicketId() { return ticketId; }
        String getName() { return name; }
        String getDestination() { return destination; }
        int getSeatNumber() { return seatNumber; }
        void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
        String getStatus() { return status; }
        void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            String seat = seatNumber == -1 ? "-" : String.valueOf(seatNumber);
            return String.format("Ticket #%d | %-12s | To: %-12s | Seat: %-3s | %s",
                    ticketId, name, destination, seat, status);
        }
    }

    // ---------- Bus manager ----------
    static class Bus {
        private final int capacity;
        private int nextTicketId = 1001;

        private final Queue<Passenger> waitingQueue = new LinkedList<>();
        private final List<Passenger> onBoard = new ArrayList<>();
        private final Map<Integer, Passenger> registry = new HashMap<>();

        Bus(int capacity) { this.capacity = capacity; }

        // Register a new passenger and add to the waiting queue
        Passenger register(String name, String destination) {
            Passenger p = new Passenger(nextTicketId++, name, destination);
            waitingQueue.offer(p);
            registry.put(p.getTicketId(), p);
            return p;
        }

        // Board passengers from the queue while seats are available
        int boardPassengers() {
            int boarded = 0;
            while (!waitingQueue.isEmpty() && onBoard.size() < capacity) {
                Passenger p = waitingQueue.poll();
                p.setSeatNumber(findFreeSeat());
                p.setStatus("ON BOARD");
                onBoard.add(p);
                boarded++;
                System.out.println("  Boarded: " + p);
            }
            if (!waitingQueue.isEmpty() && onBoard.size() >= capacity) {
                System.out.println("  Bus is full. " + waitingQueue.size() + " still waiting.");
            }
            return boarded;
        }

        // Lowest seat number not currently taken
        private int findFreeSeat() {
            Set<Integer> taken = new HashSet<>();
            for (Passenger p : onBoard) taken.add(p.getSeatNumber());
            for (int seat = 1; seat <= capacity; seat++) {
                if (!taken.contains(seat)) return seat;
            }
            return -1;
        }

        // Passenger gets off the bus
        boolean dropOff(int ticketId) {
            Passenger p = registry.get(ticketId);
            if (p == null || !onBoard.contains(p)) return false;
            onBoard.remove(p);
            p.setStatus("DROPPED OFF");
            p.setSeatNumber(-1);
            return true;
        }

        // Drop everyone going to a given stop
        int dropOffAtStop(String stop) {
            Iterator<Passenger> it = onBoard.iterator();
            int count = 0;
            while (it.hasNext()) {
                Passenger p = it.next();
                if (p.getDestination().equalsIgnoreCase(stop)) {
                    p.setStatus("DROPPED OFF");
                    p.setSeatNumber(-1);
                    it.remove();
                    count++;
                }
            }
            return count;
        }

        // Cancel a ticket (only if still waiting)
        boolean cancelTicket(int ticketId) {
            Passenger p = registry.get(ticketId);
            if (p == null || !waitingQueue.contains(p)) return false;
            waitingQueue.remove(p);
            registry.remove(ticketId);
            return true;
        }

        Passenger find(int ticketId) { return registry.get(ticketId); }

        void showQueue() {
            System.out.println("\n--- Waiting Queue (" + waitingQueue.size() + ") ---");
            if (waitingQueue.isEmpty()) System.out.println("  (empty)");
            int pos = 1;
            for (Passenger p : waitingQueue) System.out.println("  " + pos++ + ". " + p);
        }

        void showOnBoard() {
            System.out.println("\n--- On Board (" + onBoard.size() + "/" + capacity + ") ---");
            if (onBoard.isEmpty()) System.out.println("  (empty)");
            for (Passenger p : onBoard) System.out.println("  " + p);
        }

        void showAllRegistered() {
            System.out.println("\n--- All Registered Passengers ---");
            if (registry.isEmpty()) System.out.println("  (none)");
            for (Passenger p : registry.values()) System.out.println("  " + p);
        }

        // Group on-board passengers by destination using a HashMap
        void showByDestination() {
            Map<String, List<Passenger>> groups = new HashMap<>();
            for (Passenger p : onBoard) {
                groups.computeIfAbsent(p.getDestination(), k -> new ArrayList<>()).add(p);
            }
            System.out.println("\n--- On Board by Destination ---");
            if (groups.isEmpty()) System.out.println("  (empty)");
            for (Map.Entry<String, List<Passenger>> e : groups.entrySet()) {
                System.out.println("  " + e.getKey() + " (" + e.getValue().size() + "):");
                for (Passenger p : e.getValue()) {
                    System.out.println("     - " + p.getName() + " (Seat " + p.getSeatNumber() + ")");
                }
            }
        }

        int availableSeats() { return capacity - onBoard.size(); }
    }

    // ---------- Console menu ----------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Bus bus = new Bus(5); // small capacity for easy testing

        System.out.println("=== Bus Passenger Management System ===");

        while (true) {
            System.out.println("\n1. Register passenger");
            System.out.println("2. Board passengers");
            System.out.println("3. Drop off passenger (by ticket)");
            System.out.println("4. Drop off all at a stop");
            System.out.println("5. Cancel waiting ticket");
            System.out.println("6. Find passenger by ticket");
            System.out.println("7. Show waiting queue");
            System.out.println("8. Show passengers on board");
            System.out.println("9. Show on board by destination");
            System.out.println("10. Show all registered");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.print("Name: ");
                    String name = sc.nextLine().trim();
                    System.out.print("Destination: ");
                    String dest = sc.nextLine().trim();
                    if (name.isEmpty() || dest.isEmpty()) {
                        System.out.println("Name and destination are required.");
                        break;
                    }
                    Passenger p = bus.register(name, dest);
                    System.out.println("Registered with ticket #" + p.getTicketId());
                    break;
                case 2:
                    System.out.println("Seats available: " + bus.availableSeats());
                    int n = bus.boardPassengers();
                    System.out.println(n + " passenger(s) boarded.");
                    break;
                case 3:
                    System.out.print("Ticket ID: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        System.out.println(bus.dropOff(id) ? "Passenger dropped off."
                                : "Passenger not found on board.");
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ticket ID.");
                    }
                    break;
                case 4:
                    System.out.print("Stop name: ");
                    int count = bus.dropOffAtStop(sc.nextLine().trim());
                    System.out.println(count + " passenger(s) dropped off.");
                    break;
                case 5:
                    System.out.print("Ticket ID: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        System.out.println(bus.cancelTicket(id) ? "Ticket cancelled."
                                : "Ticket not found in waiting queue.");
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ticket ID.");
                    }
                    break;
                case 6:
                    System.out.print("Ticket ID: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        Passenger found = bus.find(id);
                        System.out.println(found != null ? found : "No passenger with that ticket.");
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ticket ID.");
                    }
                    break;
                case 7: bus.showQueue(); break;
                case 8: bus.showOnBoard(); break;
                case 9: bus.showByDestination(); break;
                case 10: bus.showAllRegistered(); break;
                case 0:
                    System.out.println("Goodbye!");
                    sc.close();
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
