# Aminmart Cashier Web Profile

A modern, responsive landing page for **Aminmart Cashier**, a smart cashier application designed for retail businesses and manual recording of digital sales in Indonesia.

## 🚀 Overview

Aminmart Cashier is a versatile solution for UMKM and modern retail. This web profile serves as the official landing page to showcase the application's features, provide support contact, and manage account policies.

## ✨ Key Features

- **Modern UI/UX**: Built with a clean aesthetics, glassmorphism effects, and a premium feel.
- **Feature Showcase**: Highlights core app capabilities:
  - **Digital Sales Management**: Record manual sales of Pulsa, PLN, E-Wallet, and Game Vouchers.
  - **Accurate Reporting**: Daily to yearly profit/loss monitoring.
  - **Receipt Printing**: Bluetooth thermal printer support via RawBT app.
- **Contact Support**: Dedicated contact page for user inquiries and technical support.
- **Policy Compliance**: Includes a `delete-account.html` page to comply with App Store/Play Store data deletion policies.
- **Fully Responsive**: Optimized for all devices using Tailwind CSS.

## 📥 Download & Installation

### Download Links

1. **RawBT** (Required for receipt printing): `apk/rawbt/app-release.apk`
2. **Aminmart Cashier**: `apk/cashier/app-arm64-v8a-release.apk`

### Installation Steps

1. Download and install **RawBT** application first
2. Open RawBT application and ensure the notification `Running on 127.0.0.1:8080` appears
3. Grant **Location** permission to RawBT application
4. Ensure your Bluetooth thermal printer is **powered on**
5. **Scan and pair** the Bluetooth printer in RawBT application
6. Perform a **test print** in RawBT to verify the printer is working properly
7. Download and install **Aminmart Cashier** application
8. Open Aminmart Cashier and start managing your business
9. For receipt printing, ensure RawBT is running and the printer is connected

> **Note**: RawBT is a third-party application required for printing transaction receipts via Bluetooth thermal printers.

## 🛠️ Technology Stack

- **HTML5**: Semantic structure.
- **Tailwind CSS**: Utility-first styling (via CDN).
- **Google Fonts**: [Plus Jakarta Sans](https://fonts.google.com/specimen/Plus+Jakarta+Sans) for modern typography.
- **Icons**: Emoji-based icons for fast loading and minimalist design.

## 📂 Project Structure

```text
.
├── index.html           # Main landing page (Hero, Features, Footer)
├── contact.html         # Support contact form and info
├── delete-account.html  # Account deletion request page
├── stats.html           # Statistics page
├── stats.json           # Statistics data
└── apk/
    ├── cashier/
    │   └── app-arm64-v8a-release.apk  # Aminmart Cashier APK
    └── rawbt/
        └── app-release.apk            # RawBT APK (for receipt printing)
```

## 🚀 How to Run

Since this is a static web project, you can simply open the files in your browser:

1. Clone this repository to your local machine.
2. Open `index.html` in any modern web browser (Chrome, Firefox, Safari, Edge).

## 📧 Contact

For further information or support, please reach out to:
- **Email**: official.aminmart@gmail.com
- **Website**: [Aminmart Cashier](index.html)

---

&copy; 2026 Aminmart Cashier. All rights reserved.
