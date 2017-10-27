package fr.utc.simde.payutc;

import fr.utc.simde.payutc.tools.CASConnexion;
import fr.utc.simde.payutc.tools.Dialog;
import fr.utc.simde.payutc.tools.NemopaySession;

/**
 * Created by Samy on 26/10/2017.
 */

public abstract class BaseActivity extends NFCActivity {
    private static final String LOG_TAG = "_LOG_TAG";
    protected static Dialog dialog;
    protected static NemopaySession nemopaySession;
    protected static CASConnexion casConnexion;

    protected void disconnect() {
        nemopaySession.disconnect();
        casConnexion.disconnect();
    }

    protected void unregister() {
        nemopaySession.unregister();
        disconnect();

        dialog.errorDialog(getString(R.string.key_registration), getString(R.string.key_remove_temp));
    }
}
