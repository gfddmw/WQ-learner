# WQ Learner Design

Date: 2026-05-11

## Goal

WQ Learner is an Android app for 11408 postgraduate entrance exam review. The first version lets a user upload photos of wrong questions, recognize the content into Markdown with LaTeX formulas, classify each question by 11408 subject and chapter, save it to a cloud question bank, and practice later by either drawing existing wrong questions or requesting simulated variant questions.

The selected first-version scope is a semi-real product:

- Real Android capture or image selection.
- Real backend upload, user account, cloud question bank, and OCR/formula-recognition integration boundary.
- Real storage of recognized content as Markdown plus LaTeX.
- Real 11408 subject and chapter classification with user correction.
- Simulated variant-question generation behind the same API shape that a future large model will use.

## Non-Goals

- The first version will not call a production large model for variant questions.
- The first version will not build a web client.
- The first version will not attempt fully local OCR or formula recognition on Android.
- The first version will not implement fine-grained knowledge-point tagging beyond subject and chapter.

## Architecture

The app uses a cloud-centered architecture:

- Android App: user interaction, photo capture, image selection, recognition review, manual correction, question browsing, and practice.
- FastAPI Backend: authentication, upload handling, OCR/formula-recognition orchestration, classification, question-bank APIs, and practice APIs.
- Cloud Data: user records, mistake questions, uploaded images, category metadata, and practice records.
- OCR/Formula Recognition Provider: returns recognized text and formulas, which the backend normalizes into Markdown plus LaTeX.
- Future LLM Provider: later replaces the simulated variant-question service without changing the Android flow.

This architecture keeps API keys and model-service credentials out of the Android app, supports account-based sync, and leaves room for future multi-device or web clients.

## Android App

The Android app uses four main tabs:

1. Upload
2. Question Bank
3. Practice
4. Me

### Upload Flow

The upload flow is the core entry path:

1. User takes a photo or chooses an image from the album.
2. App uploads the image to the backend.
3. Backend stores the image and runs OCR/formula recognition.
4. Backend returns a draft question with recognized Markdown plus LaTeX and suggested subject/chapter.
5. App shows the original image and editable recognition result side by side or in stacked sections.
6. User corrects the content, subject, and chapter.
7. App confirms the question and saves it to the cloud question bank.

### Question Bank

The question bank supports:

- Browsing by subject and chapter.
- Searching recognized question text.
- Opening a question detail page with original image, Markdown/LaTeX content, subject, chapter, status, and mastery level.
- Editing the recognized content and category after saving.

The first version classifies questions into the four 11408 subjects:

- Data Structures
- Computer Organization
- Operating Systems
- Computer Networks

Each subject has a curated chapter list. Examples include trees and graphs for Data Structures, cache and instruction pipeline for Computer Organization, process management and memory management for Operating Systems, and TCP/IP and routing for Computer Networks.

### Practice

Practice has two modes:

- Original mode: draw existing wrong questions from the cloud question bank.
- Variant mode: call the variant-question API. In the first version, this returns simulated variants based on the selected source question and preserves the future LLM response shape.

After answering or reviewing, the user records a result such as unfamiliar, reviewing, or mastered. These values update the question's mastery state and can later influence draw weights.

### Me

The Me tab includes:

- Login and logout state.
- Account information.
- Basic sync status.
- Future API/provider settings if needed during development.

## Backend API

The backend is built with Python and FastAPI.

### Authentication

- `POST /auth/register`: create a user account.
- `POST /auth/login`: return an access token.
- `GET /me`: return the current user profile.

### Questions

- `POST /questions/upload`: accept an image upload, store it, run recognition/classification, and return a draft.
- `POST /questions/{id}/confirm`: save the user's corrected Markdown/LaTeX, subject, chapter, and metadata.
- `GET /questions`: list questions with filters for subject, chapter, mastery, status, and search text.
- `GET /questions/{id}`: return one question with image URL and content.
- `PATCH /questions/{id}`: update recognized content, category, or mastery state.

### Practice

- `POST /practice/original`: draw existing wrong questions using filters and optional count.
- `POST /practice/variant`: return a simulated variant question for one or more source questions.
- `POST /practice/{id}/review`: record the user's review result.

## Data Model

### User

- `id`
- `email`
- `password_hash`
- `created_at`

### MistakeQuestion

- `id`
- `user_id`
- `image_url`
- `content_md_latex`
- `subject`
- `chapter`
- `status`
- `mastery`
- `created_at`
- `updated_at`

`content_md_latex` is the canonical recognized and corrected question body. It can include Markdown paragraphs, code-style snippets when needed, inline formulas such as `$O(n \log n)$`, and block formulas such as `$$...$$`.

### PracticeSession

- `id`
- `user_id`
- `mode`: `original` or `variant`
- `question_ids`
- `variant_payload`
- `result`
- `created_at`
- `reviewed_at`

`variant_payload` stores the simulated variant response in the first version and will later store the real LLM output.

## OCR and Classification Pipeline

The upload pipeline is:

1. Validate image size and type.
2. Store the image in object storage or a development storage directory.
3. Call OCR/formula-recognition provider.
4. Normalize provider output into Markdown plus LaTeX.
5. Run subject/chapter classification.
6. Return a draft question to the app.

The first classifier uses a rule-based keyword map with confidence scoring and user correction. It is intentionally simple and inspectable. Example hints:

- Data Structures: tree, binary tree, graph, hash, heap, B-tree, sorting, search.
- Computer Organization: cache, pipeline, instruction cycle, addressing mode, ALU, memory hierarchy.
- Operating Systems: process, thread, semaphore, deadlock, scheduling, virtual memory, page replacement.
- Computer Networks: TCP, UDP, IP, routing, subnet, congestion control, HTTP, DNS.

If classification confidence is low, the backend still returns the recognized draft, but the app asks the user to choose subject and chapter before confirming.

## Error Handling

- Upload failure: app shows retry and does not create a question.
- Image stored but recognition failed: backend creates a draft with image URL and failed status so the user can retry recognition or fill content manually.
- OCR returns poor output: user can correct Markdown/LaTeX before saving.
- Classification confidence is low: app requires manual subject and chapter selection.
- Authentication expires: app returns to login and keeps local unsaved upload state when feasible.

## Testing Scope

### Android

- Login/logout state.
- Photo or image-selection flow.
- Upload progress and retry states.
- Recognition review and manual correction.
- Question-bank filters.
- Original and variant practice mode switching.

### Backend

- Authentication and authorization.
- Upload validation.
- Question draft creation and confirmation.
- Question listing filters.
- Rule-based classification.
- Simulated variant-question response shape.
- Permission checks so users can access only their own questions and practice sessions.

## Implementation Notes

The existing project is a minimal Android Jetpack Compose app. The implementation should keep changes incremental:

1. Establish app navigation and screen shells.
2. Add backend project separately.
3. Define shared API contracts.
4. Implement authentication and question upload.
5. Add recognition and classification boundaries.
6. Add question bank and practice flows.

The current workspace is not a git repository, so this design document cannot be committed until git is initialized or the project is placed inside a repository.
