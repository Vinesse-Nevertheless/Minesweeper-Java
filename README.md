# High-Performance Minesweeper Engine (Java)

## 🎯 Overview
This repository contains a robust, object-oriented Minesweeper engine built with Java. The application is designed with a focus on algorithmic efficiency, clean separation of concerns, and a "fail-safe" user experience. Key features include a recursive-safe area reveal and a guaranteed safe first move through deferred grid generation.

---

## 🛠 Technical Architecture & Design Patterns

### **1. The Dispatcher-Orchestrator Pattern**
The engine utilizes a `Dispatcher` class to manage the application lifecycle. This separates the initial configuration (mine count validation) from the active game loop, allowing for a clean transition from the setup phase to the interactive gameplay phase.

### **2. Lazy Initialization (First-Click Safety)**
To ensure professional-grade gameplay, the engine implements **Lazy Initialization**. 
*   **The Logic**: The internal board (mine placement and neighbor-clue calculation) is not generated until the player issues their first `free` command.
*   **The Benefit**: The player's initial coordinates are passed to the generator to be explicitly excluded from the mine-distribution pool, guaranteeing that the first move is never a mine.

### **3. Algorithmic Exploration (Queue-Based BFS)**
For the "cascade" effect—where clicking an empty cell reveals a large safe area—the engine employs a **Breadth-First Search (BFS)** algorithm.
*   **Implementation**: Utilizes a `Queue<List<Integer>>` and a `seen[][]` boolean matrix to track visited cells.
*   **Stability**: By opting for an iterative BFS over a recursive Depth-First Search (DFS), the engine avoids `StackOverflowError` risks, making it stable for significantly larger grid dimensions.

### **4. State Management & Data Structures**
*   **Dual-Board System**: Maintains a `savedBoard` (the ground truth) and a `currBoard` (the player's visible UI). This allows for non-destructive cell marking (`*`) and unmarking without losing the underlying neighbor-clue data.
*   **Optimized Lookups**: Uses `HashSet<List<Integer>>` for mine coordinate tracking, ensuring **O(1) constant-time complexity** for verifying moves against mine locations.

---

## 🚀 Key Features
*   **Two Winning Conditions**: Supports winning by either correctly flagging all mines or revealing every safe cell on the board.
*   **Dual-Input Commands**: Supports `free` (reveal) and `mine` (flag/unflag) commands via a Cartesian coordinate system.
*   **Input Robustness**: Includes a `Validation` layer to handle malformed input, out-of-bounds coordinates, and invalid mine density requests.

---

## 💻 Tech Stack
*   **Language**: Java 17+
*   **Concepts**: OOP, BFS (Breadth-First Search), Lazy Initialization, State Management, Java Collections Framework (Set, Queue, LinkedList).

---

## 📥 Getting Started
1. **Clone**: `git clone https://github.com/yourusername/minesweeper-java.git`
2. **Compile**: `javac minesweeper/*.java`
3. **Run**: `java minesweeper.Main`

## License
This project is licensed under the [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/) License - see the LICENSE file for details.

![Java](https://img.shields.io/badge/language-Java-orange)
![License](https://img.shields.io/badge/license-CC%20BY--NC%204.0-blue)
![AI-No-Training](https://img.shields.io/badge/AI-No--Training-red)
