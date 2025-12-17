# Sepsafe
Temporal machine learning framework for early sepsis risk prediction based on multivariate ICU time-series data, leveraging a stacked (LSTM) architecture. Beyond retrospective evaluation, the model was deployed within a lightweight mobile application, enabling real-time inference, and visualization of short-term risk trajectories. 

# Sepsafe: Early Sepsis Prediction App

This repository contains the complete implementation of *Sepsafe*, a mobile and web-based system for early sepsis risk prediction using a stacked LSTM model.

## Project Structure
- `android-app/`: Android application for real-time inference
- `ml-model/`: Model training scripts and exported TensorFlow Lite model
- `backend-api/`: Optional backend service for inference

## Requirements
- Android Studio Flamingo or newer
- Android SDK 33+
- Python 3.9 (for model training only)

## How to run
1. Clone the repository:
   https://github.com/emm1403/Sepsafe.git
2. Open Android Studio -> Open -> select the project folder
3. Let Gradle sync
4. Run on emulator or physical device

## Model
The app uses a TensorFlow Lite model stored in:
app/src/main/assets/

