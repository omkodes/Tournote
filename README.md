# 📱 Tournote Android App

**Tournote** is an all-in-one mobile app designed to make group travel seamless, collaborative, and fun. 🌍 Organize groups, chat in real time, manage trip expenses, share memories, plan routes, and track your friends—all in one place.

---

## 📑 Table of Contents

1. [Features](#features)  
   - [Group Management](#1-group-management)  
   - [Real-Time Chat](#2-real-time-chat)  
   - [Expense Management](#3-expense-management)  
   - [Memories (Photo Sharing)](#4-memories-photo-sharing)  
   - [Smart Route Planner](#5-smart-route-planner)  
   - [Track Friends](#6-track-friends)  
   - [Onboarding & Authentication](#7-onboarding--authentication)  
2. [Tech Stack](#️-tech-stack)  
3. [Setup and Installation](#setup-and-installation)  
4. [Usage](#usage)  
5. [Screenshots](#️-screenshots)  
6. [Contributing](#contributing)  
7. [License](#license)  
8. [Contact](#contact)


---

## 🚀 Features

### 1. 👥 Group Management
- **Create Groups:** Start a new travel group, invite members, and assign admins.  
- **Group Profiles:** Manage group details and view all member roles.  
- **Owner Controls:** Assign or revoke admin privileges for group members.

### 2. 💬 Real-Time Chat
- **Instant Messaging:** Send and receive messages instantly within your travel group.  
- **Socket.io Integration:** Enables low-latency communication with reliable message delivery.  
- **Push Notifications:** Get notified of new messages via Firebase Cloud Messaging (FCM). 🔔  
- **Message History:** Securely load past conversations using REST API.

### 3. 💰 Expense Management
- **Add & Split Expenses:** Record shared costs and split them equally, by percentage, or exact amounts.  
- **Settle Debts:** Track who owes whom and mark payments as settled. ✅  
- **Expense Overview:** View a summary of all group expenses, breakdowns, and balances.  
- **Auto-Detection:** Automatically detect expenses and repayments from SMS (e.g., bank alerts). 📱  
- **Location Tracking:** Save the location where each transaction occurred.

### 4. 📸 Memories (Photo Sharing)
- **Upload Photos:** Share trip photos with your group.  
- **Gallery View:** Browse group photos in albums or full-screen mode. 🖼️  
- **Background Uploads:** Uploads are handled reliably using WorkManager.  
- **Sort Memories:** Sort photos and videos by the date taken.  
- **Download All:** Download all media as a ZIP file. 📦

### 5. 🗺️ Smart Route Planner
- **Waypoints:** Add, remove, and reorder trip stops.  
- **Geocoding:** Get location suggestions and map each stop. 🔍  
- **Optimal Routing:** Calculate the shortest route covering all waypoints.  
- **Interactive Maps:** Visualize your routes in an embedded WebView map.  
- **Redirect to Google Maps:** Get directions for the finalized route and share it.

### 6. 📍 Track Friends
- **Live Location:** Share and view real-time locations of group members. 🌐  
- **Map View:** Display all friends on an interactive map using HTML assets.  
- **Safety Alerts:** Send and receive safety alerts within the group. 🚨  
- **Background Location Service:** Location tracking continues even when the app is minimized.  
- **End Trip:** Automatically revoke location permissions at the end of a trip.

### 7. 🔐 Onboarding & Authentication
- **Animated Splash & Guided Onboarding:** Welcome new users and highlight key features. ✨  
- **Sign Up / Log In:** Secure authentication via email and password using Firebase.  
- **Offline Support:** Friendly offline messages with retry mechanisms. 📶

---

## 🛠️ Tech Stack

| Category             | Technologies / Tools                                                                 |
|----------------------|--------------------------------------------------------------------------------------|
| **Languages & UI**   | Kotlin, XML, JavaScript, HTML, CSS                                                  |
| **Architecture**     | MVVM, Single Source of Truth, ViewModel, Room (Local DB), Shared Preferences        |
| **Backend & Realtime** | Firebase Realtime Database, Firebase Cloud Messaging, Firebase Authentication      |
| **Maps & Geolocation**| Leaflet.js, Leaflet Routing Machine, Leaflet Control Geocoder, FusedLocationProviderClient |
| **Networking & Sync**| WebSocket, Node.js + Express, WorkManager, BroadcastReceiver                        |
| **Media & UI Utils** | Glide, Cloudinary, MediaPlayer, Notification Channel, Inline Reply Notification     |
| **Persistence**      | Room Database, Shared Preferences                                                   |
| **WebView Features** | JavaScript Interface, Geocoding & Reverse Geocoding, Regex                          |
| **Other**            | DiffUtil, PostgreSQL, Foreground Services                                           |

---

## ⚡ Setup and Installation

1. **Clone the repository:** 📥
   ```bash
   git clone https://github.com/your-org/tournote.git
   ```

2. **Open the project in Android Studio.** 💻

3. **Firebase Setup:** 🔥
   - Download your `google-services.json` file from Firebase Console and place it in the `/app` directory.

4. **Sync Gradle and build the project.** ⚙️

5. **Run the app on an emulator or Android device.** 🚀

---

## 📖 Usage

1. **Create or Join a Group:** Start by creating or joining a group for your trip.

2. **Group Chatting:** Use the chat tab to coordinate with your group.

3. **Manage Expenses:** Record and split expenses effortlessly.

4. **Share Memories:** Upload and browse trip photos in the memories section.

5. **Plan Routes:** Use the route planner to visualize your travel itinerary.

6. **Track Friends:** Enable location sharing to view friends on the map.

---

## 🖼️ Screenshots

Add screenshots of the main screens below:

- Group selection and onboarding
- Chat interface
- Expense dashboard and split screen
- Memories gallery
- Route planning map
- Friend tracker map

---

## 🤝 Contributing

We welcome contributions! Please feel free to submit a Pull Request.

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.

---

## 👨‍💻 Team

Built by:

- **Parth Shroff** - [GitHub](https://github.com/parth-shroff)
- **Omkar Sanap** - [GitHub](https://github.com/omkar-sanap)

---

## 📬 Contact

📧 **Parth:** parthpshroff@gmail.com — [GitHub](https://github.com/parth-shroff)

📧 **Omkar:** sanapomkar685@gmail.com — [GitHub](https://github.com/omkar-sanap)

🔗 **GitHub Project:** [Tournote](https://github.com/your-org/tournote)

---

**Tournote** — Making group adventures smarter and more connected! 🌟
