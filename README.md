# Nutriority 🥗💪

**Your Intelligent Partner for Personalized Nutrition and Fitness Excellence.**

Nutriority is a cutting-edge Android application designed to bridge the gap between dietary planning and physical performance. By leveraging user-specific data, the app generates rolling 7-day meal plans, tracks workouts, and provides actionable insights for fitness goals.

---

## 🔗 Quick Links

*   **📚 Repository:** [github.com/nexonsabanao/Capstone](https://github.com/nexonsabanao/Capstone)
*   **📥 Installer:** [nutriority.github.io/Installer](https://nutriority.github.io/Installer)
*   **🔧 Admin Dashboard:** [nutriority.github.io/Admin](https://nutriority.github.io/Admin)

---

## 🚀 Features

### 🥗 Personalized Nutrition
*   **Rolling 7-Day Meal Plan:** A dynamic meal schedule that stays stable throughout the week, featuring "Yesterday", "Today", and "Tomorrow" labels for easy tracking.
*   **Meal Swapping:** Don't like a recommendation? Swap it for a nutritionally equivalent meal that matches your diet type.
*   **Comprehensive Logging:** Log meals from your plan or add custom foods manually.
*   **Macro Tracking:** Visual progress bars for Protein, Carbs, and Fats based on TDEE (Total Daily Energy Expenditure) calculations.

### 🏋️ Intelligent Fitness
*   **Official & Custom Workouts:** Choose from expertly designed official workouts or create your own custom routines.
*   **Active Workout Tracking:** A real-time timer, exercise completion tracking, and automatic rest-period management.
*   **Integrated Warmups/Cooldowns:** Toggleable sessions to ensure you prepare and recover correctly.
*   **Calorie Burn Calculation:** Advanced formulas using MET values, duration, and your current weight for high accuracy.

### 📊 Progress & Analytics
*   **Interactive Weight Chart:** Powered by MPAndroidChart, visualize your weight trends over time.
*   **Activity History:** A calendar-based view showing your workout streaks and consistency.
*   **Real-time Profile Stats:** Centralized dashboard for total calories burned, minutes exercised, and workouts completed.

### 🛠 Precise User Controls
*   **Tactile Ruler Selection:** Custom-built `RulerView` for highly precise weight and height selection.
*   **Dual Unit Support:** Seamlessly switch between Metric (kg, cm) and Imperial (lbs, ft'in") systems with instant conversions.

---

## 🛠 Technology Stack

*   **Language:** Kotlin
*   **Architecture:** MVVM (Model-View-ViewModel) with a clean Repository pattern.
*   **Database:** 
    *   **Room:** Local persistence for offline-first capabilities and lightning-fast data retrieval.
    *   **Firebase Firestore:** Real-time cloud synchronization across devices.
*   **Authentication:** Firebase Auth (Email/Password & Google Sign-In).
*   **Asynchronous Logic:** Kotlin Coroutines & Flow (StateFlow/SharedFlow) for reactive UI updates.
*   **Dependency Injection:** Dagger Hilt for robust and scalable module management.
*   **UI Components:**
    *   **Navigation:** Single-activity architecture with a ViewPager2-based Tab navigation.
    *   **Image Loading:** Glide for efficient image and GIF caching.
    *   **Graphs:** MPAndroidChart for performance visualization.
    *   **Animations:** Konfetti for workout completion celebrations.

---

## 📁 Architecture Overview

The app follows a modern Android architecture to ensure testability and maintainability:

1.  **UI Layer:** Fragments (e.g., `ProfileFragment`, `MealFragment`) observing `StateFlow` from ViewModels.
2.  **ViewModel Layer:** Manages UI state and business logic (e.g., calculating rolling dates or filtering today's meals).
3.  **Repository Layer:** Acts as a mediator between the local Room DB and the Firebase cloud source.
4.  **Planner Service:** A dedicated logic module for calculating TDEE, macros, and generating balanced weekly plans.

---

## ⚙️ Setup & Installation

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/nexonsabanao/Capstone.git
    ```
2.  **Firebase Configuration:**
    *   Create a project in the [Firebase Console](https://firebase.google.com/).
    *   Add your Android app's package name (`com.example.nutriority`).
    *   Download `google-services.json` and place it in the `app/` directory.
    *   Enable **Firestore**, **Authentication**, and **Storage**.
3.  **Build the Project:**
    *   Open the project in Android Studio.
    *   Sync Gradle and ensure all dependencies are resolved.
    *   Run the app on a physical device or emulator (API 26+).

---

## 🤝 Contributing

Contributions are welcome! If you'd like to improve the diet algorithms or UI responsiveness:
1.  Fork the Project.
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the Branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.

**Nutriority** — *Priority for your Nutrition.*
