# 🧠 MindMosaic – An AI-Powered Journal App for Mood Tracking & Emotional Insights

MindMosaic is an Android journaling app that uses **AI (DeepSeek R1)** to analyze your journal entries and provide emotional feedback, personalized motivational quotes, summaries, and activity suggestions. All journal data is securely stored in **Firebase**, and mood trends are visualized using bar charts.

> 📱 Developed using **Kotlin + XML**, powered by **DeepSeek R1** via OpenRouter, and designed as part of an open-ended Android development project.

---

## 🌟 Features

### ✍️ Journal Fragment
- Users write daily journal entries.
- Each entry is analyzed by **DeepSeek R1 AI Model** to generate:
  - 🎭 Mood (Happy, Sad, Angry, Anxious, Excited, Neutral)
  - 📝 Summary
  - 💬 Motivational Quote
  - 🌱 Relaxation Activity
- Analysis and data are stored in **Firebase Firestore** under the authenticated user.

### 📊 Insights Fragment
- Displays a **bar chart** of mood scores from the past 7 days.
- If multiple entries exist in a day, their mood scores are **averaged**.
- Shows:
  - Total journal entries
  - Most common mood
  - Emotional trend over time

---

## 🛠️ Tech Stack

| Layer           | Technology Used                  |
|----------------|----------------------------------|
| Frontend        | Kotlin + XML                     |
| AI Integration  | DeepSeek R1 (via OpenRouter)     |
| Backend         | Firebase Authentication + Firestore |
| Charting        | MPAndroidChart                   |
| Architecture    | MVVM (with coroutine support)    |

---

## 🔐 Permissions & Security

- Firestore rules restrict data access:
  - Users can only read/write their own entries.
- Authenticated access enforced via `request.auth.uid`.

---

🚀 Getting Started
Clone this repo
git clone https://github.com/zainbaig313/MindMosaic-An-AI-Powered-Journal-App-for-Mood-Track


Open in Android Studio
Open the folder as a new Android project
Sync Gradle
Set up Firebase with your own credentials (via Firebase Console)
Add your OpenRouter API key in EmotionRepository.kt

💡 Future Improvements
Exporting PDF summaries of weekly moods
Dark mode support
Offline journal saving with sync

🙋‍♂️ About Me
Hi, I'm Zain Baig 👋
This app was developed as part of an open-ended university project under the guidance of our professor. I'm currently open to internships in Android development and AI-integrated mobile apps.

📬 Feel free to connect or suggest improvements!



