# AI Usage Documentation
### - SiriValli MC25BT021

**Assigned Module:** Grading & Assessment (`com.campus.cms.grading`) and Integration/Testing.  
**Primary AI Tool Used:** Google Gemini (single continuous development & troubleshooting session).

---

## 1. Summary of How AI Was Used

### A. Schema & Cross-Module Integration Verification
- **What I asked AI:** Verified how my grading module classes (`Assignment`, `AssignmentDAO`, `Grade`, `GradeDAO`, `GradeService`, `GradesPanel`) aligned with the SQLite schema (`db/schema.sql`) and `StudentDashboard.java`.
- **What AI helped clarify:**
  - Confirmed that `db/schema.sql` stores optional file attachments in the `material_path` column of the `assignments` table rather than a separate `materials` table.
  - Verified that `StudentDashboard.java` renders `new GradesPanel(e.getCourseId(), false, studentId)` in student mode and `FacultyDashboard.java` renders `new GradesPanel(selected.getCourseId(), true, getFacultyId())` in faculty mode.

### B. Implementing the 5 Additional Grading Module Files
- **What I asked AI:** Requested implementations for the remaining 5 specification files (`Material.java`, `AssignmentsPanel.java`, `StudentAssignmentsPanel.java`, `StudentGradesPanel.java`, and `FacultyReportsPanel.java`) so that all 11 files in `com.campus.cms.grading` match the project specification tree.
- **Debugging & Manual Fixes Applied:**
  - In `FacultyReportsPanel.java`, fixed a compilation error where `Grade::isPublished` was initially generated instead of checking the integer flag `g.getPublished() == 1` defined in `Grade.java` (`0 = Draft`, `1 = Published`).
  - Verified clean compilation across all 11 Java files using `mvn compile` and tested the live Swing UI with seeded credentials (`student1` / `pass123`).

### C. Git Workflow & Repository Synchronization
- **What I asked AI:** Troubleshooting Git branch synchronization when pulling updates from `origin/main` after teammates merged `FacultyDashboard.java` and other modules.
- **Actions Taken:** Pulled remote updates cleanly without re-cloning, kept `StudentDashboard.java` untouched to avoid merge conflicts, and staged/pushed only `src/main/java/com/campus/cms/grading/`.

---

## 2. Representative Prompts Used in This Session
1. *"What happened about those 5 missing files (`Material`, `AssignmentsPanel`, `StudentAssignmentsPanel`, `StudentGradesPanel`, `FacultyReportsPanel`)?"*
2. *"Now again some branches are pushed to main, how do I get them into my existing folder?"*
3. *"I have to implement these 5 missing files—generate them matching our `AssignmentDAO` and `GradeDAO`."*
4. *"One faculty file (`FacultyReportsPanel.java`) has errors with `Grade.java`—how do I fix it?"*

---

## 3. Verification & Academic Integrity Statement
- Every AI-generated Java file was compiled locally with Maven (`mvn compile`), verified against `db/schema.sql` and `db/seed.sql`, and tested by running `com.campus.cms.Main`.
- No AI-generated code was committed blindly; type mismatches (`int` vs `boolean` for `published`) and file paths were manually checked and fixed prior to pushing to `origin/main`.
