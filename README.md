# Japanese Test Bank Management VN

## Introduction

The **Japanese Test Bank Management VN** application helps teachers and learners to input, store, and export exams and answer keys. The system integrates AI to extract questions and suggest answers from images, supports manual editing, manages audio files, and exports test sets to PDF/DOC files with separate answer sheets.

---

## System Overview

The application follows a multi-layer architecture with clear separation of concerns:

```
[ User ]
   ↓
[ UI Layer (Swing Forms)]
   ↓
[ Service Layer ]
   ↓
[ DAO Layer ]
   ↓
[ MySQL Database ]
   ↓
[ AI Service (OCR, NLP)]
```

- **UI Layer**: Interface for inputting, managing questions and tests, exporting files, working with images/questions.
- **Service Layer**: Handles business logic, AI extraction, exporting tests, answer suggestions, audio file management.
- **DAO Layer**: CRUD operations for tests, questions, answers, audio files via JDBC.
- **AI Service**: Integrates AI/ML for OCR to extract questions from images and automatically generate answers.
- **Database**: Stores tests, answers, audio file paths, statistics.

---

## Main Features

### 1. Home & Test Statistics

<img width="1357" height="767" alt="Ảnh chụp màn hình 2025-09-03 182128" src="https://github.com/user-attachments/assets/a654c9a1-e514-4248-8907-1e0fa8357a88" />


- Overview of N5/N4/N3/N2 test sets and the number of attempts.
- Intuitive interface for easy navigation.

---

### 2. Question Management

<img width="1358" height="769" alt="Ảnh chụp màn hình 2025-09-03 182135" src="https://github.com/user-attachments/assets/de8a910d-1b30-430b-b2ed-cba3e282d32e" />


- Add, edit, delete questions manually.
- Input questions from images and extract content using AI (OCR + NLP).
- Automatically suggest answers with editable options.
- Support for multimedia questions (images, audio).
- Search questions by content, type, difficulty.

#### AI Extraction from Images

<img width="1580" height="969" alt="Ảnh chụp màn hình 2025-09-03 182244" src="https://github.com/user-attachments/assets/f8cb7370-7f6e-41b2-b3ff-15ea5fd7a3ce" />




- Upload test images, AI extracts question content and answers, allowing manual adjustments and attribute setup (difficulty/type).

#### Add New Question Manually
<img width="1421" height="903" alt="Ảnh chụp màn hình 2025-09-03 182301" src="https://github.com/user-attachments/assets/57241dae-bcc8-443b-b8f0-0729142af802" />


- Specify question type, level, content, answers, font format, size, and attach audio/image files if needed.

---

### 3. Test Management

<img width="1374" height="780" alt="Ảnh chụp màn hình 2025-09-03 182237" src="https://github.com/user-attachments/assets/49bfbcad-9c47-4004-91a8-8f1d6cbbf1ec" />


- Create, edit, delete tests, view details.
- Generate random tests from the question bank.
- Export tests to PDF/DOC (each test has a separate answer file).
- Manage tests with audio files: store audio locally, save file path to DB.
- Statistics on question count per test, creation date.

#### Create New Test

<img width="671" height="360" alt="Ảnh chụp màn hình 2025-09-03 182314" src="https://github.com/user-attachments/assets/67469496-314e-4a85-97ba-3cf501f3902d" />


- Enter test name, description, select questions from the bank to include in the test.

#### Generate Random Test


<img width="1442" height="820" alt="Ảnh chụp màn hình 2025-09-03 182317" src="https://github.com/user-attachments/assets/29c2feec-bdb0-416c-b9bf-97b3299dbb10" />


- Create a random test by difficulty and name, supporting multiple test sets.

#### Generate Test by Criteria
<img width="1439" height="831" alt="Ảnh chụp màn hình 2025-09-03 182310" src="https://github.com/user-attachments/assets/0dabddd8-5c9d-4e82-9772-bac2baa53cf6" />


- Enter test name, select difficulty, and set the number of questions for each skill (Grammar, Listening, Reading, Vocabulary, Kanji).

---

### 4. Audio File Management

- Upload and store audio files for questions/tests on disk.
- Audio file paths saved in the DB, with download/delete options.

---

### 5. Export Tests

- Choose test structure, number of tests to generate, randomize from the question bank.
- Export PDF/DOC files containing test content and separate answer keys.
- Support exporting with audio files if available.

---

### 6. Interface Settings

<img width="1413" height="817" alt="Ảnh chụp màn hình 2025-09-03 182326" src="https://github.com/user-attachments/assets/d48913c0-0910-4bbd-b4be-a8d470e157cd" />


- Choose between light or dark UI theme for user comfort and flexibility.

---

## Workflow Overview

1. **Input Questions**
   - Manually or extract via AI from images.
   - Set attributes, difficulty, type, multimedia (audio/image).

2. **Manage Questions/Tests**
   - Edit, delete, search, categorize.

3. **Create Tests**
   - Select questions or generate random tests.
   - Save tests and export to PDF/DOC with separate answer keys.

4. **Manage Multimedia Files**
   - Store audio/images locally, save paths in DB.

5. **Customize Interface**
   - Change theme as needed.

---

## Data Flow Diagram

```
[Input Questions/Tests] → [AI Extraction (images)] → [Save to DB + audio] → [Manage questions/tests] → [Export PDF/DOC + answer keys]
```

---

## Technologies Used

- **Java Swing**: User Interface.
- **MySQL**: Stores tests, questions, audio file paths.
- **AI/OCR**: Extracts questions from images, suggests answers.
- **File System**: Stores audio files locally.
- **Apache POI/iText**: Export DOC/PDF tests and answers.

---

> **Note:** The system ensures security, access control, and supports both manual and AI-powered operations.

---

## System Illustrations
