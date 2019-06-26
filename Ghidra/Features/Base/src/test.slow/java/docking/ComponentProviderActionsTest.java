/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package docking;

import static org.junit.Assert.*;

import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.*;

import org.junit.*;

import docking.action.DockingActionIf;
import docking.action.KeyBindingData;
import docking.actions.KeyEntryDialog;
import docking.tool.util.DockingToolConstants;
import ghidra.framework.options.ToolOptions;
import ghidra.framework.plugintool.PluginTool;
import ghidra.test.AbstractGhidraHeadedIntegrationTest;
import ghidra.test.TestEnv;
import ghidra.util.Msg;
import ghidra.util.SpyErrorLogger;
import resources.Icons;
import resources.ResourceManager;

public class ComponentProviderActionsTest extends AbstractGhidraHeadedIntegrationTest {

	// note: this has to happen after the test framework is initialized, so it cannot be static
	private final Icon ICON = ResourceManager.loadImage("images/refresh.png");
	private static final String PROVIDER_NAME = "Test Action Provider";
	private static final KeyStroke CONTROL_T =
		KeyStroke.getKeyStroke(Character.valueOf('t'), DockingUtils.CONTROL_KEY_MODIFIER_MASK);

	private TestEnv env;
	private PluginTool tool;
	private TestActionsComponentProvider provider;
	private SpyErrorLogger spyLogger = new SpyErrorLogger();

	@Before
	public void setUp() throws Exception {
		env = new TestEnv();
		tool = env.launchDefaultTool();
		provider = new TestActionsComponentProvider(tool);

		Msg.setErrorLogger(spyLogger);
	}

	@After
	public void tearDown() {
		env.dispose();
	}

	@Test
	public void testIcon_WithIcon_BeforeAddedToTool() {

		setIcon(ICON);

		showProvider();

		assertWindowMenuActionHasIcon(ICON);
	}

	@Test
	public void testIcon_WithIcon_AfterAddedToTool() {

		setIcon(null);

		showProvider();
		assertWindowMenuActionHasIcon(Icons.EMPTY_ICON);

		setIcon(ICON);
		assertWindowMenuActionHasIcon(ICON);
	}

	@Test
	public void testIcon_WithoutIcon() {
		showProvider();
		assertWindowMenuActionHasIcon(Icons.EMPTY_ICON);
	}

	@Test
	public void testSetKeyBinding_DirectlyOnProvider() {
		//
		// This is how clients set key bindings on providers, as desired, when constructing them
		//

		KeyStroke defaultKs = CONTROL_T;
		setDefaultKeyBinding(defaultKs);

		showProvider();

		assertProviderKeyStroke(defaultKs);
		assertOptionsKeyStroke(defaultKs);
		assertMenuItemHasKeyStroke(defaultKs);
	}

	@Test
	public void testSetKeyBinding_DirectlyOnProvider_TransientProvider() {
		//
		// Transient providers cannot have key bindings
		//
		switchToTransientProvider();

		setErrorsExpected(true);
		setDefaultKeyBinding(CONTROL_T);
		setErrorsExpected(false);

		showProvider();

		spyLogger.assertLogMessage("Transient", "cannot", "key", "binding");
	}

	@Test
	public void testSetTransientAfterSettingKeyBinding() {

		setDefaultKeyBinding(CONTROL_T);

		setErrorsExpected(true);
		switchToTransientProvider();
		setErrorsExpected(false);

		showProvider();

		spyLogger.assertLogMessage("Transient", "not", "key", "binding");
	}

	@Test(expected = IllegalStateException.class)
	public void testSetTransientAfterAddedProviderToTheTool() {

		showProvider();
		switchToTransientProvider(); // exception
	}

	@Test
	public void testSetKeyBinding_ViaDialog_FromWindowMenu() {

		showProvider();

		KeyStroke newKs = CONTROL_T;
		setKeyBindingViaF4Dialog_FromWindowsMenu(newKs);

		assertProviderKeyStroke(newKs);
		assertOptionsKeyStroke(newKs);
		assertMenuItemHasKeyStroke(newKs);
	}

	@Test
	public void testSetKeyBinding_ViaOptions_WithoutToolbarAction() {

		showProvider();

		KeyStroke newKs = CONTROL_T;
		setOptionsKeyStroke(newKs);

		assertProviderKeyStroke(newKs);
		assertOptionsKeyStroke(newKs);
		assertMenuItemHasKeyStroke(newKs);
	}

	@Test
	public void testSetKeyBinding_ViaOptions_WithToolbarAction() {

		showProvider();

		KeyStroke newKs = CONTROL_T;
		setOptionsKeyStroke(newKs);

		assertProviderKeyStroke(newKs);
		assertOptionsKeyStroke(newKs);
		assertMenuItemHasKeyStroke(newKs);
	}

	@Test
	public void testSetKeyBinding_ViaDialog_FromToolBar() {

		setToolbarIcon(ICON);
		showProvider();

		KeyStroke newKs = CONTROL_T;
		setKeyBindingViaF4Dialog_FromToolsToolbar(newKs);

		assertProviderKeyStroke(newKs);
		assertOptionsKeyStroke(newKs);
		assertMenuItemHasKeyStroke(newKs);
	}

	@Test
	public void testSetKeyBinding_TransientProvider_CannotBeSetFromWindowMenu() {

		switchToTransientProvider();
		showProvider();

		assertCannotShowKeyBindingDialog_FromWindowsMenu();
	}

	@Test
	public void testSetKeyBinding_TransientProvider_CannotBeSetFromToolbar() {

		switchToTransientProvider();

		setErrorsExpected(true);
		setToolbarIcon(ICON);
		setErrorsExpected(false);

		spyLogger.assertLogMessage("Transient", "not", "toolbar");
	}

	@Test
	public void testSetIcon_NullIconWithToolbarAction() {

		try {
			setToolbarIcon(null);
			fail("Expected an exception passing a null icon when specifying a toolbar action");
		}
		catch (Exception e) {
			// expected
		}
	}

//==================================================================================================
// Private Methods
//==================================================================================================

	private void assertShowProviderActionIsInToolbar() {
		assertNotNull("The 'Show Provider' action is not in the toolbar",
			getToolbarShowProviderAction());
	}

	private void assertShowProviderActionNotInToolbar() {
		assertNull("The 'Show Provider' action is in the toolbar", getToolbarShowProviderAction());
	}

	private void switchToTransientProvider() {
		provider.setTransient();
	}

	private void showProvider() {
		provider.addToTool();
		tool.showComponentProvider(provider, true);
		waitForSwing();
	}

	private void setDefaultKeyBinding(KeyStroke defaultKs) {
		runSwing(() -> provider.setDefaultKeyBinding(new KeyBindingData(defaultKs)));
	}

	private void setIcon(Icon icon) {
		runSwing(() -> provider.setIcon(icon));
	}

	private void setToolbarIcon(Icon icon) {
		runSwing(() -> provider.setIcon(icon, true));
	}

	private DockingActionIf getShowProviderAction() {

		DockingActionIf showProviderAction =
			getAction(tool, provider.getOwner(), provider.getName());
		assertNotNull("Could not find action to show ", showProviderAction);
		return showProviderAction;
	}

	private DockingActionIf getWindowMenuShowProviderAction() {
		waitForSwing();
		DockingActionIf action = waitFor(() -> {
			Set<DockingActionIf> actions = getActionsByName(tool, PROVIDER_NAME);

			//@formatter:off
			return actions
				.stream()
				.filter(a -> a.getOwner().equals(DockingWindowManager.DOCKING_WINDOWS_OWNER))
				.findFirst()
				.get()
				;
			//@formatter:on
		});

		assertNotNull("Window menu action not installed for provider", action);
		assertTrue(action.getClass().getSimpleName().contains("ShowComponentAction"));
		return action;
	}

	private DockingActionIf getToolbarShowProviderAction() {

		DockingWindowManager dwm = tool.getWindowManager();
		ActionToGuiMapper guiActions =
			(ActionToGuiMapper) getInstanceField("actionToGuiMapper", dwm);
		GlobalMenuAndToolBarManager toolbarManager =
			(GlobalMenuAndToolBarManager) getInstanceField("menuAndToolBarManager", guiActions);
		DockingActionIf action = provider.getShowProviderAction();
		DockingActionIf toolbarAction = toolbarManager.getToolbarAction(action.getName());
		return toolbarAction;
	}

	private void setOptionsKeyStroke(KeyStroke newKs) {
		ToolOptions keyOptions = tool.getOptions(DockingToolConstants.KEY_BINDINGS);

		// shared option name/format: "Provider Name (Tool)" - the shared action's owner is the Tool
		runSwing(() -> keyOptions.setKeyStroke(provider.getName() + " (Tool)", newKs));
		waitForSwing();
	}

	private void assertProviderKeyStroke(KeyStroke expectedKs) {

		DockingActionIf action = getShowProviderAction();
		KeyStroke actionKs = action.getKeyBinding();
		assertEquals(expectedKs, actionKs);
	}

	private void assertOptionsKeyStroke(KeyStroke expectedKs) {

		ToolOptions options = getKeyBindingOptions();

		// Option name: the action name with the 'Tool' as the owner
		String fullName = provider.getName() + " (Tool)";
		KeyStroke optionsKs = runSwing(() -> options.getKeyStroke(fullName, null));
		assertEquals("Key stroke in options does not match expected key stroke", expectedKs,
			optionsKs);
	}

	private void assertWindowMenuActionHasIcon(Icon expected) {
		DockingActionIf action = getWindowMenuShowProviderAction();
		assertEquals("Windows menu icons for provider does not match the value set on the provider",
			expected, action.getMenuBarData().getMenuIcon());
	}

	private void assertCannotShowKeyBindingDialog_FromWindowsMenu() {
		// simulate the user mousing over the 'Window' menu's action
		DockingActionIf windowMenuAction = getWindowMenuShowProviderAction();
		DockingWindowManager.setMouseOverAction(windowMenuAction);

		performLaunchKeyStrokeDialogAction();
		DialogComponentProvider warningDialog = waitForDialogComponent("Unable to Set Keybinding");
		close(warningDialog);
	}

	private void setKeyBindingViaF4Dialog_FromWindowsMenu(KeyStroke ks) {

		// simulate the user mousing over the 'Window' menu's action
		DockingActionIf windowMenuAction = getWindowMenuShowProviderAction();
		DockingWindowManager.setMouseOverAction(windowMenuAction);

		performLaunchKeyStrokeDialogAction();
		KeyEntryDialog dialog = waitForDialogComponent(KeyEntryDialog.class);

		runSwing(() -> dialog.setKeyStroke(ks));

		pressButtonByText(dialog, "OK");

		assertFalse("Invalid key stroke: " + ks, runSwing(() -> dialog.isVisible()));
	}

	private void assertMenuItemHasKeyStroke(KeyStroke expected) {

		DockingActionIf action = getWindowMenuShowProviderAction();
		assertEquals(
			"Windows menu key binding for provider does not match the value of the provider",
			expected, action.getKeyBinding());
	}

	private void setKeyBindingViaF4Dialog_FromToolsToolbar(KeyStroke ks) {

		// simulate the user mousing over the 'Window' menu's action		
		DockingActionIf toolbarAction = getToolbarShowProviderAction();
		assertNotNull("Provider action not installed in toolbar", toolbarAction);
		DockingWindowManager.setMouseOverAction(toolbarAction);

		performLaunchKeyStrokeDialogAction();
		KeyEntryDialog dialog = waitForDialogComponent(KeyEntryDialog.class);

		runSwing(() -> dialog.setKeyStroke(ks));

		pressButtonByText(dialog, "OK");

		assertFalse("Invalid key stroke: " + ks, runSwing(() -> dialog.isVisible()));
	}

	private void performLaunchKeyStrokeDialogAction() {
		DockingWindowManager dwm = tool.getWindowManager();
		ActionToGuiMapper actionMapper = dwm.getActionToGuiMapper();
		Action action = actionMapper.getDockingKeyAction(KeyStroke.getKeyStroke("F4"));
		assertNotNull(action);
		runSwing(() -> action.actionPerformed(new ActionEvent(this, 0, "")), false);
	}

	private ToolOptions getKeyBindingOptions() {
		return tool.getOptions(DockingToolConstants.KEY_BINDINGS);
	}

	private class TestActionsComponentProvider extends ComponentProvider {

		private JComponent component = new JTextField("Hey!");

		TestActionsComponentProvider(DockingTool tool) {
			super(tool, PROVIDER_NAME, "Fooberry Plugin");
		}

		@Override
		public JComponent getComponent() {
			return component;
		}

	}
}
