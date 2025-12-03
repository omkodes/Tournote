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
- **Finalised Route:** Publish finalised route to intimate to other group members.

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

<p float="left">
  <img src="https://github.com/user-attachments/assets/3981904e-eb80-4e74-9cc6-e7477fa9479d" width="250" />
  <img src="https://github.com/user-attachments/assets/3a98d777-6eca-45da-a863-a2c086e1c992" width="250" />
  <img src="https://github.com/user-attachments/assets/560fb1d9-f198-401d-ac48-d8f0402e15f7" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/33c0c1dd-2612-49b8-8900-97c11ef19398" width="250" />
  <img src="https://github.com/user-attachments/assets/2b82beb0-8a60-49f7-bdd7-f87acc95ac9a" width="250" />
  <img src="https://github.com/user-attachments/assets/805398fa-9049-43fa-8fb3-18918f616db1" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/6708ff35-b52a-4b35-86c7-839004df34ca" width="250" />
  <img src="https://github.com/user-attachments/assets/d1ccbc4b-79d7-47bc-b22c-06dbd8aba6e2" width="250" />
  <img src="https://github.com/user-attachments/assets/3c43a3ce-fc04-4147-9b27-385dc5edd455" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/972c629c-d070-4ef5-a0cd-3abdc50c9c8d" width="250" />
  <img src="https://github.com/user-attachments/assets/ba428ec5-ed4c-458f-aa39-5bc149860dc7" width="250" />
  <img src="https://github.com/user-attachments/assets/e90ed0b4-712d-4b55-87cc-92e68663a92d" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/765a8f7a-0d39-4b54-9a10-730b3388229f" width="250" />
  <img src="https://github.com/user-attachments/assets/f2d8458c-2770-45ce-aaf4-6dc5f2bf7679" width="250" />
  <img src="https://github.com/user-attachments/assets/0c42152d-31f5-4b29-8255-03e8082cccd7" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/73c0ea9b-7dea-449e-b68e-f26b12463204" width="250" />
  <img src="https://github.com/user-attachments/assets/9fb9f0fd-39c5-4c9d-a14f-9d8903e81845" width="250" />
  <img src="https://github.com/user-attachments/assets/727f3c0e-baea-4757-8442-d4ed56d9032e" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/cbd7526f-cb12-4191-8347-697e784df055" width="250" />
  <img src="https://github.com/user-attachments/assets/4ff1ae45-fe4c-4fc9-b54f-548941a77b93" width="250" />
  <img src="https://github.com/user-attachments/assets/98aa13e1-0fe9-4d68-90a1-b275862c289b" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/51502c16-4df1-4d33-ab33-d4acea6dfd8e" width="250" />
  <img src="https://github.com/user-attachments/assets/c472ee51-9d49-40cd-b8a2-4f002d3eba4b" width="250" />
  <img src="https://github.com/user-attachments/assets/819b92c8-be79-49ef-af65-9ea9a60796c9" width="250" />
</p>

<p float="left">
  <img src="https://github.com/user-attachments/assets/c3b45691-a0b3-4039-b9c5-b893288a23d7" width="250" />
  <img src="https://github.com/user-attachments/assets/9805791d-7f6a-4cc0-b55c-ad0921ac6608" width="250" />
  <img src="https://github.com/user-attachments/assets/57d628ef-56e2-4bdd-aa6a-21a5ffb455b8" width="250" />
</p>


---

## 🤝 Contributing

We welcome contributions! Please feel free to submit a Pull Request.

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.

---

## 👨‍💻 Team

Built by:

- **Parth Shroff** - [Linkedin](https://www.linkedin.com/in/parth-shroff-0655ba320?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app)
- **Omkar Sanap** - [Linkedin](https://www.linkedin.com/in/omkar-sanap-app?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app)

---

## 📬 Contact

📧 **Parth:** parthpshroff@gmail.com — [GitHub](https://github.com/parthpranav2)

📧 **Omkar:** sanapomkar685@gmail.com — [GitHub](https://github.com/Omkarsanap-19)

🔗 **GitHub Project:** [Tournote](https://github.com/Parthpranav2/Tournote)

---

**Tournote** — Making group adventures smarter and more connected! 🌟
