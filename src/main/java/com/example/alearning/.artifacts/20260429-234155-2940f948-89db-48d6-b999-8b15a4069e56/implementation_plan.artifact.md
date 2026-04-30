# Add Athletes to Group from Group Tab

Enable adding athletes to a group directly from the Group tab in the Roster screen for a more consistent and intuitive experience.

## User Review Required

- **ManageGroupMembersSheet UI**: I will implement a new sheet that allows searching and selecting athletes to add to or remove from a specific group. This is consistent with the interaction model but new for the Groups tab.

## Proposed Changes

### [Roster Component]

#### [RosterViewModel.kt](file:///C:/Users/APF/AndroidStudioProjects/Alearning/app/src/main/java/com/example/alearning/ui/roster/RosterViewModel.kt)

- Add `showManageMembersDialog: String? = null` (storing the Group ID) to `RosterUiState`.
- Add actions to `RosterAction`:
    - `OnShowManageMembersDialog(groupId: String)`
    - `OnDismissManageMembersDialog`
    - `OnAddAthleteToGroup(groupId: String, individualId: String)`
    - `OnRemoveAthleteFromGroup(groupId: String, individualId: String)` (Note: This already exists as `OnRemoveAthleteFromGroup` but I'll make sure it's used correctly).
- Handle the new actions in `onAction`.

#### [RosterScreen.kt](file:///C:/Users/APF/AndroidStudioProjects/Alearning/app/src/main/java/com/example/alearning/ui/roster/RosterScreen.kt)

- Update `ModernGroupCard` to include an "Add Athlete" button in its expanded state.
- Implement `ManageGroupMembersSheet` at the bottom of the file (or as a private composable) to allow adding/removing members from a group.
- Update `RosterContent` to show `ManageGroupMembersSheet` when `uiState.showManageMembersDialog` is not null.

## Verification Plan

### Automated Tests
- No new automated tests are planned as this is primarily a UI/State change.
- Existing build and unit tests will be run.

### Manual Verification
- Deploy to emulator.
- Navigate to the **Groups** tab.
- Expand a group.
- Verify the "Add Athlete" button is present.
- Tap "Add Athlete" and verify the `ManageGroupMembersSheet` appears.
- Search for an athlete and add them to the group.
- Verify the athlete is added to the group list and the group count updates.
- Verify removing an athlete still works and shows confirmation.
