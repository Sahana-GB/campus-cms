# AI usage declaration: Kavya (Dashboard Integration, UI Design System, and Synthetic Data Generator)


## Tool
Antigravity / Gemini (Google DeepMind), used through the agentic IDE and chat interface.

## What I used it for
- Designing and implementing the cohesive UI design system (`com.campus.cms.core.ui`): `Theme.java` (color palette, typography, spacing, border radii), `Styles.java` (button variants, table/input helpers, action bars), `SidebarNav.java` (collapsible navigation with user avatar chip and pinned logout), `PageHeader.java`, `FilterBar.java` (with `WrapLayout`), `StatusBadge.java` (custom rounded badge renderer), and `DashboardCard.java` (accent stripe metric cards with hover feedback).
- Overhauling and modernizing all three role dashboards (`AdminDashboard`, `FacultyDashboard`, and `StudentDashboard`), ensuring visual consistency, responsive resizing, and strict role guards.
- Building the scalable, deterministic Synthetic Student Data Generator (`python/generate_data.py`), its unit test suite (`python/test_generate_data.py`), and documentation (`python/README.md`).
- Diagnosing and resolving GUI layout issues: fixing button clipping on Quick Access action bars (`Styles.actionBar()` with wrapping layout) and auto-populating enrolled students in `GradesPanel.java` so faculty can grade newly created assignments.
- Resolving Git merge conflicts when integrating `feature/ui-design-system` into `main`, eliminating duplicate method definitions and variable declarations while keeping test coverage intact.
- Authoring LaTeX report sections and comprehensive automated test suites.

## What I did myself
- Defined the visual design language (modern Indigo/Slate academic SaaS aesthetic) and chose design tokens matching the project requirements.
- Validated the SQLite schema constraints, foreign key cascades, and table relationships against `db/schema.sql` to ensure the synthetic data generator complies with all integrity rules.
- Implemented and verified exact password hash compatibility with Java's `PasswordUtil` (PBKDF2-HMAC-SHA256, 10,000 iterations, 16-byte salt, 32-byte key) so generated student accounts can authenticate seamlessly into the Java app.
- Ran all Git merges, resolved conflicts across conflicting dashboard branches, and verified clean repository state.
- Executed the synthetic data generator at scale (100, 1,000, and 10,000 students) and provided the generated dataset used by Person C for query profiling.
- [ ] Ran the application (`java -jar target/campus-cms.jar`) and manually tested all 3 portals (Admin, Faculty, Student) with test credentials and synthetic logins.

## How I checked the AI's work
- Ran full automated test suites:
  - Java: `mvn clean test` (136/136 tests passing, including tests for `AdminDashboardTest`, `FacultyDashboardTest`, `StudentDashboardTest`, `DashboardCardTest`, `StudentDAOTest`, `StudentsPanelTest`).
  - Python: `python -m unittest python/test_generate_data.py` (11/11 tests passing, validating PBKDF2 hash vectors, foreign keys, transaction rollbacks, idempotency, and 10,000-record scale performance).
- Code reviews: Inspected generated Swing code to ensure thread safety (using `SwingUtilities.invokeLater` and `EventQueue`), proper layout managers, and zero regression on existing DAO/Service layers.
- Checked database integrity via `PRAGMA foreign_key_check` and `PRAGMA integrity_check` after running the generator.
- [ ] Manual testing in the application: verified responsive window resizing, sidebar navigation switching cards, theme contrast, and student profile password changes.

## Things the AI got wrong or I changed
- During initial dashboard refactoring, the Quick Access panel in `AdminDashboard` had a rigid `maximumSize` that caused second-row action buttons to clip vertically; fixed this by replacing static sizing with a custom dynamic wrapping action bar (`Styles.actionBar()`).
- In `GradesPanel`, newly created assignments initially displayed an empty table with no rows because grades hadn't been recorded yet; updated `GradeDAO` and `GradesPanel` to automatically pull all approved enrolled students from the course so faculty can immediately enter marks and feedback.
- Cleaned up duplicate button declarations (`gotoProfileBtn`) and trailing curly braces introduced during branch merge conflict resolution.
- Adjusted font references in `Theme.java` to use standardized `Theme.SMALL` and `Theme.SMALL_BOLD` tokens instead of undefined ad-hoc constants.
- [ ] Add anything else you modified or rejected.

## Understanding
I understand and can explain all aspects of my implementation during the viva and demo, including the Swing layout hierarchy, the UI design tokens, the PBKDF2 hashing algorithm parity, and the synthetic data generator's transaction architecture.
