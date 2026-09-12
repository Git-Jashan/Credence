# 🛡️ Credence: Privacy-Preserving Alternative Underwriting & Purpose-Bound Credit Rails

> **Smart India Hackathon** | Problem Statement: **SIH25150** (Beneficiary Credit Scoring)
> *100% On-Device Heuristic Evaluation • Zero Cloud Data Ingestion • DPDP Act 2023 Compliant*

---

## 📌 Repository Ecosystem

Credence is engineered as a decentralized tri-party ecosystem connecting borrowers, statutory authorities, and institutional capital providers:

| Component | Platform / Tech Stack | Repository & Access |
| :--- | :--- | :--- |
| **📱 Credence Mobile App (Borrower)** | Android (Kotlin, Jetpack Compose) | [📍 This Repository](./) |
| **🏛️ Government Scheme Dashboard** | Web (Tailwind, React) | [🔗 View Govt Portal Repo](https://github.com/Vipul1217/Government-Dashboard) |
| **🏦 Institutional Lender Terminal** | Web (Tailwind, React) | [🔗 View Lender Portal Repo](https://github.com/Vipul1217/Private-lender-dashboard) |

---

## 💡 The Core Problem & Solution

* **The Problem:** Over 50 Crore micro-merchants and street vendors in India are excluded from formal credit because traditional bureaus (CIBIL/Experian) rely on historical debt records. Meanwhile, existing fintech apps illegally scrape SMS and email in violation of RBI Digital Lending norms and the DPDP Act 2023.
* **The Solution:** **Credence** executes **100% on-device heuristic extraction** directly from password-protected bank statement PDFs. Raw financial documents never leave the merchant's hardware. The engine derives an explainable 0–100 **Trust Profile** and enforces capital utilization via **Purpose-Bound Trust Cards** (NFC/QR) to eliminate fund diversion.

---

## 🏗️ System Architecture

```text
                                  ┌─────────────────────────────┐
                                  │   Merchant Smartphone       │
                                  │  (Kotlin / Jetpack Compose) │
                                  └──────────────┬──────────────┘
                                                 │
                               ┌─────────────────┴─────────────────┐
                               │ On-Device Heuristic Regex Engine  │
                               │  - PDF Decryption & Parsing       │
                               │  - 6-Factor Risk Extraction       │
                               │  - Zero Cloud Footprint           │
                               └─────────────────┬─────────────────┘
                                                 │
                                                 ▼
                             ┌──────────────────────────────────────┐
                             │ Cryptographic Zero-Knowledge Profile │
                             │      (Portable Score / QR Hash)      │
                             └──────────────┬──────────────┬────────┘
                                            │              │
                     ┌──────────────────────┘              └──────────────────────┐
                     ▼                                                            ▼
      ┌──────────────────────────────┐                             ┌──────────────────────────────┐
      │   Government Dashboard       │                             │  Institutional Lender Portal │
      │   (PM SVaNidhi Oversight)    │                             │  (NBFC Underwriting)         │
      ├──────────────────────────────┤                             ├──────────────────────────────┤
      │ • Real-time fund tracing     │                             │ • Deterministic risk bands   │
      │ • Anomaly & mismatch alerts  │                             │ • Algorithmic counter-offers │
      │ • Whitelisted MCC validation │                             │ • Automated ledger updates   │
      │ • One-click account freeze   │                             │ • Performance-based score Δ  │
      └──────────────────────────────┘                             └──────────────────────────────┘
```

---

## ✨ Key Architectural Innovations

### 1. 🔒 100% On-Device Zero-Knowledge Ingestion

* **Zero Cloud Storage:** PDFs are decrypted, parsed, and scrubbed in ephemeral local memory. No raw statements, transactions, or passwords ever touch our backend servers.
* **Regulatory Moat:** Meets statutory mandates under the **Digital Personal Data Protection (DPDP) Act 2023** and **RBI Digital Lending Guidelines**.

### 2. 💳 Purpose-Bound Trust Cards (NFC & Dynamic QR)

* Micro-loans frequently default due to **capital leakage** (diverting business loans to non-productive expenses).
* Credence issues closed-loop virtual cards restricted strictly to verified Merchant Category Codes (MCC) (e.g., raw materials, equipment).
* Unauthorized merchant transactions (e.g., luxury retail, personal electronics) are declined at the point-of-sale in real-time.

### 3. 🤝 Dynamic Bilateral Negotiation Engine

* Replaces binary "Approve / Reject" workflows.
* Borrowers broadcast desired loan amounts and interest rates while viewing real-time approval odds.
* Lenders review objective cashflow diagnostics and issue algorithmic **counter-offers** (adjusted tenure, rate, or principal).

---

## 📊 Objective 6-Factor Underwriting Model

Credence replaces black-box scoring with deterministic, explainable metrics derived from peer-reviewed econometric literature:

1. **Income Consistency (20%):** Measures month-on-month volatility of incoming credits.
2. **Transaction Velocity (15%):** Quantifies active trading frequency and business turnover.
3. **Inflow / Outflow Ratio (20%):** Evaluates operating surplus and liquidity cushions.
4. **Financial Longevity (15%):** Assesses historical depth of transaction continuity.
5. **Payer Diversity (15%):** Mitigates single-client dependency risk through distinct UPI counter-parties.
6. **Lean-Period Resilience (15%):** Stress-tests cashflow health during documented low-earning cycles.

---

## 📚 Empirical & Statutory Anchors

* **Algorithmic Validation:** *Alok et al. (Indian School of Business)* — Demonstrated that digital payment footprints predict default risk with higher fidelity than traditional credit bureau scores for thin-file demographics.
* **Behavioural Modeling:** *Djeundje et al. (Expert Systems with Applications)* — Validated the statistical power of non-traditional indicators for credit decisioning.
* **Policy Alignment:** **PM SVaNidhi** (Ministry of Housing and Urban Affairs) & **DPDP Act 2023** (Ministry of Electronics and Information Technology).

---

## 🚀 Quickstart & Setup

### 📱 Android Client (This Repository)

```bash
# Open in Android Studio
# Build and assemble debug APK
./gradlew assembleDebug
```

* **Requirements:** Android SDK 34+, Java 17, Android Studio Hedgehog or newer.

---

## 👥 Team Credence

* **Project Status:** Working Prototype (Ready to Build, Run & Grow)
