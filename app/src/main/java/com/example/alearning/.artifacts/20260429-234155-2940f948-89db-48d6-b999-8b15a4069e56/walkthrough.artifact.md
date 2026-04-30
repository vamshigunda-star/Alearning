# Roster Screen UX Redesign Walkthrough

I have redesigned the Roster screen to improve intuitiveness and usability, following the provided wireframe.

## Key Changes

### 1. Tabbed Navigation
The Roster screen is now split into two main tabs: **Athletes** and **Groups**. This separates individual management from organization, reducing mental load.

### 2. Modern Athlete Management
- **Athlete Cards**: Redesigned with initials, full name, age, sex, and group tags.
- **Multi-Select**: Added checkboxes for selecting multiple athletes. A contextual "Add to Group" bar appears when one or more athletes are selected.
- **Swipe-to-Delete**: Athletes can be swiped left to delete, which triggers a safe confirmation dialog.
- **Search**: Both tabs now include a search bar for quick filtering.

### 3. Improved Group Management
- **Expandable Group Cards**: Groups are shown as cards that can be expanded to see the member list inline.
- **Avatar Stacks**: Collapsed group cards show a stacked avatar list of members.
- **Member Management**: Within an expanded group, members can be removed (with confirmation).

### 4. Safer Destructive Actions
Added confirmation dialogs for:
- Deleting an athlete.
- Removing a member from a group.

## Screenshots

````carousel
![Athletes Tab](/C:/Users/APF/AndroidStudioProjects/Alearning/app/src/main/java/com/example/alearning/.artifacts/20260429-234155-2940f948-89db-48d6-b999-8b15a4069e56/athletes_tab.png)
<!-- slide -->
![Groups Tab Expanded](/C:/Users/APF/AndroidStudioProjects/Alearning/app/src/main/java/com/example/alearning/.artifacts/20260429-234155-2940f948-89db-48d6-b999-8b15a4069e56/groups_tab.png)
<!-- slide -->
![Delete Confirmation](/C:/Users/APF/AndroidStudioProjects/Alearning/app/src/main/java/com/example/alearning/.artifacts/20260429-234155-2940f948-89db-48d6-b999-8b15a4069e56/delete_confirmation.png)
````

## Verification Summary
- **Manual Testing**: Verified tab switching, searching, selection, expansion, and swipe actions on an emulator.
- **Build**: Successfully compiled with `:app:compileDebugKotlin`.
