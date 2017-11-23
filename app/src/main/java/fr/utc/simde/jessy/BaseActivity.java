package fr.utc.simde.jessy;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.utc.simde.jessy.responses.BottomatikResponse;
import fr.utc.simde.jessy.tools.Bottomatik;
import fr.utc.simde.jessy.tools.CASConnexion;
import fr.utc.simde.jessy.tools.Config;
import fr.utc.simde.jessy.tools.Dialog;
import fr.utc.simde.jessy.tools.Ginger;
import fr.utc.simde.jessy.tools.HTTPRequest;
import fr.utc.simde.jessy.tools.NemopaySession;

/**
 * Created by Samy on 26/10/2017.
 */

public abstract class BaseActivity extends InternetActivity {
    private static final String LOG_TAG = "_BaseActivity";

    private static final String gitUrl = "https://raw.githubusercontent.com/simde-utc/jessy/master/";
    private static final String manifestUrl = "app/src/main/AndroidManifest.xml";
    private static final String downloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";

    protected static NemopaySession nemopaySession;
    protected static Ginger ginger;
    protected static Bottomatik bottomatik;
    protected static CASConnexion casConnexion;

    protected static Config config;
    protected static Dialog dialog;

    protected static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 24) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        this.dialog = new Dialog(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dialog.dismiss();
    }

    protected void disconnect() {
        nemopaySession.disconnect();
        casConnexion.disconnect();
    }

    protected void unregister(final Activity activity) {
        nemopaySession.unregister();
        disconnect();

        dialog.errorDialog(activity, getString(R.string.key_registration), getString(R.string.key_remove_temp));
    }

    protected void fatal(final Activity activity, final String title, final String message) {
        dialog.fatalDialog(activity, title, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                unregister(activity);
                restartApp(activity);
            }
        });
    }

    protected void hasRights(final String title, final String[] rightList, final Runnable runnable) { hasRights(title, rightList, false, runnable);}
    protected void hasRights(final String title, final String[] rightList, final boolean needToBeSuper, final Runnable runnable) {
        dialog.startLoading(BaseActivity.this, getString(R.string.information_collection), getString(R.string.user_rights_list_collecting));
        new Thread() {
            @Override
            public void run() {
                try {
                    List<String> rights = Arrays.asList(rightList);
                    List<String> sameRights = new ArrayList<String>();
                    nemopaySession.getAllMyRights();
                    Thread.sleep(100);

                    JsonNode myRightList = nemopaySession.getRequest().getJSONResponse();

                    if (myRightList.has("0")) {
                        for (JsonNode myRight : myRightList.get("0")) {
                            if (rights.contains(myRight.textValue()))
                                sameRights.add(myRight.textValue());
                        }
                    }

                    if (!needToBeSuper) {
                        for (JsonNode foundation : myRightList) {
                            for (JsonNode myRight : foundation) {
                                if (rights.contains(myRight.textValue()) && !sameRights.contains(myRight.textValue()))
                                    sameRights.add(myRight.textValue());
                            }
                        }
                    }

                    if ((rights.size() == sameRights.size() && rights.size() != 0) || (rights.size() == 0 && myRightList.has("0") && myRightList.get("0").size() > 75)) // Si on a plus de 75 droits sur toutes les fondations, on estime qu'on a le full access
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                runnable.run();
                            }
                        });
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                dialog.errorDialog(BaseActivity.this, title, nemopaySession.forbidden(rightList, needToBeSuper));
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();
                            dialog.errorDialog(BaseActivity.this, getString(R.string.user_rights_list_collecting), getString(R.string.error_rights));
                        }
                    });
                }

            }
        }.start();
    }

    protected void restartApp(final Activity activity) {
        try {
            PackageManager pm = getPackageManager();
            Intent mStartActivity = pm.getLaunchIntentForPackage(
                getPackageName()
            );
            mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            int mPendingIntentId = 223344;
            PendingIntent mPendingIntent = PendingIntent.getActivity(activity, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

            System.exit(0);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    protected void startMainActivity(final Activity activity) {
        disconnect();

        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        finish();
        activity.startActivity(intent);
    }

    protected void startFoundationListActivity(final Activity activity) {
        if (config.getFoundationId() != -1) {
            startSellActivity(activity);
            return;
        }

        dialog.startLoading(activity, getString(R.string.information_collection), getString(R.string.foundation_list_collecting));
        final Intent intent = new Intent(activity, FoundationsOptionsActivity.class);

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getFoundations();
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode foundationList = request.getJSONResponse();

                    if (!request.isJSONResponse() || !foundationList.isArray())
                        throw new Exception("Malformed JSON");

                    for (final JsonNode foundation : foundationList) {
                        if (!foundation.has("name") || !foundation.has("fun_id"))
                            throw new Exception("Unexpected JSON");
                    }

                    intent.putExtra("foundationList", request.getResponse());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (activity instanceof FoundationsOptionsActivity)
                                finish();

                            activity.startActivity(intent);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(R.string.foundation_list_collecting), e.getMessage());
                        }
                    });
                }
            }
        }.start();
    }

    public void startSellActivity(final Activity activity) {
        startArticleGroupActivity(activity, new Intent(activity, SellActivity.class));
    }

    public void startSellBottomatikActivity(final Activity activity) {
        if (activity instanceof SellByBottomatikActivity)
            finish();

        startActivity(new Intent(activity, SellByBottomatikActivity.class));
    }

    public void startEditActivity(final Activity activity) {
        startArticleGroupActivity(activity, new Intent(activity, EditActivity.class));
    }

    public void startArticleGroupActivity(final Activity activity, final Intent intent) {
        dialog.startLoading(activity, activity.getResources().getString(R.string.information_collection), getString(R.string.location_list_collecting));

        new Thread() {
            @Override
            public void run() {
                try {
                    if (config.getLocationId() == -1) {
                        intent.putExtra("categoryListAuthorized", new ArrayList<Integer>());
                        intent.putExtra("keyboardListAuthorized", new ArrayList<Integer>());
                    }
                    else {
                        nemopaySession.getLocations();
                        Thread.sleep(100);

                        // Toute une série de vérifications avant de lancer l'activité
                        final HTTPRequest request = nemopaySession.getRequest();
                        final JsonNode locationList = request.getJSONResponse();

                        if (locationList == null || locationList.size() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.stopLoading();

                                    fatal(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.location_error_0));
                                }
                            });

                            return;
                        }

                        if (!locationList.isArray())
                            throw new Exception("Malformed JSON");

                        ArrayList<Integer> categoryListAuthorized = new ArrayList<Integer>();
                        ArrayList<Integer> keyboardListAuthorized = new ArrayList<Integer>();
                        for (final JsonNode location : locationList) {
                            if (!location.has("id") || !location.has("name") || !location.has("enabled") || !location.has("categories") || !location.has("sales_keyboards"))
                                throw new Exception("Unexpected JSON");

                            if (location.get("id").intValue() == config.getLocationId()) {
                                for (JsonNode category : location.get("categories"))
                                    categoryListAuthorized.add(category.intValue());

                                for (JsonNode keyboard : location.get("sales_keyboards"))
                                    keyboardListAuthorized.add(keyboard.intValue());
                            }
                        }

                        intent.putIntegerArrayListExtra("categoryListAuthorized", categoryListAuthorized);
                        intent.putIntegerArrayListExtra("keyboardListAuthorized", keyboardListAuthorized);
                    }
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(R.string.location_list_collecting), e.getMessage());
                        }
                    });
                }

                if (config.getLocationId() != -1 || config.getInCategory()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.changeLoading(getString(R.string.category_list_collecting));
                        }
                    });

                    try {
                        nemopaySession.getCategories();
                        Thread.sleep(100);

                        // Toute une série de vérifications avant de lancer l'activité
                        final HTTPRequest request = nemopaySession.getRequest();
                        final JsonNode groupList = request.getJSONResponse();

                        if (!groupList.isArray())
                            throw new Exception("Malformed JSON");

                        if (config.getLocationId() == -1 && groupList.size() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.stopLoading();

                                    dialog.errorDialog(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.category_error_0));
                                }
                            });

                            return;
                        }

                        intent.putExtra("categoryList", request.getResponse());
                    } catch (final Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(activity, getString(R.string.category_list_collecting), e.getMessage());
                            }
                        });
                    }
                }

                if (config.getLocationId() != -1 || !config.getInCategory()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.changeLoading(getString(R.string.keyboard_list_collecting));
                        }
                    });

                    try {
                        nemopaySession.getKeyboards();
                        Thread.sleep(100);

                        // Toute une série de vérifications avant de lancer l'activité
                        final HTTPRequest request = nemopaySession.getRequest();
                        final JsonNode groupList = request.getJSONResponse();

                        if (!groupList.isArray())
                            throw new Exception("Malformed JSON");

                        if (config.getLocationId() == -1 && groupList.size() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.stopLoading();

                                    dialog.errorDialog(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.keyboard_error_0));
                                }
                            });

                            return;
                        }

                        intent.putExtra("keyboardList", request.getResponse());
                    } catch (final Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(activity, getString(R.string.keyboard_list_collecting), e.getMessage());
                            }
                        });
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.changeLoading(getResources().getString(R.string.article_list_collecting));
                    }
                });

                try {
                    int responseCode = nemopaySession.getArticles();
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode articleList = request.getJSONResponse();

                    if (!request.isJSONResponse() || !articleList.isArray())
                        throw new Exception("Malformed JSON");

                    if (articleList.size() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                dialog.errorDialog(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.article_error_0));
                            }
                        });

                        return;
                    }

                    for (final JsonNode article : articleList) {
                        if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id") || article.get("fundation_id").intValue() != nemopaySession.getFoundationId())
                            throw new Exception("Unexpected JSON");
                    }

                    intent.putExtra("articleList", request.getResponse());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (activity instanceof ArticleGroupActivity)
                                finish();

                            activity.startActivity(intent);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(R.string.article_list_collecting), e.getMessage());
                        }
                    });
                }
            }
        }.start();
    }

    protected void startBuyerInfoActivity(final Activity activity, final String badgeId) {
        dialog.startLoading(activity, activity.getResources().getString(R.string.information_collection), activity.getResources().getString(R.string.buyer_info_collecting));
        final Intent intent = new Intent(activity, BuyerInfoActivity.class);

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getBuyerInfo(badgeId);
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode buyerInfo = request.getJSONResponse();

                    if (!request.isJSONResponse())
                        throw new Exception("Malformed JSON");

                    if (!buyerInfo.has("lastname") || !buyerInfo.has("username") || !buyerInfo.has("firstname") || !buyerInfo.has("solde") || !buyerInfo.has("last_purchases") || !buyerInfo.get("last_purchases").isArray())
                        throw new Exception("Unexpected JSON");

                    intent.putExtra("badgeId", badgeId);
                    intent.putExtra("buyerInfo", request.getResponse());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (activity instanceof BuyerInfoActivity)
                                finish();

                            activity.startActivity(intent);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    try {
                        if (nemopaySession.getRequest().getResponseCode() == 400)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.errorDialog(activity, getString(R.string.information_collection), getString(R.string.badge_error_not_recognized));
                                }
                            });
                        else
                            throw new Exception("");

                        return;
                    } catch (Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(activity, getString(R.string.information_collection), e.getMessage());
                            }
                        });
                    }
                }
            }
        }.start();
    }

    protected void startCardManagementActivity(final Activity activity) {
        hasRights(getString(R.string.user_rights_list_collecting), new String[]{"GESUSERS"}, true, new Runnable() {
            @Override
            public void run() {
            activity.startActivity(new Intent(activity, CardManagementActivity.class));
            }
        });
    }

    protected void startQRCodeReaderActivity(final Activity activity) {
        if (haveCameraPermission())
            startActivity(new Intent(activity, QRCodeReaderActivity.class));
        else
            dialog.errorDialog(BaseActivity.this, getString(R.string.qrcode), getString(R.string.need_camera_permission));
    }

    protected void delKey() {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove("key");
        edit.apply();

        unregister(BaseActivity.this);
    }

    protected void setNemopayKey(final String key) {
        if (key.equals(""))
            return;

        dialog.startLoading(BaseActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_authentification));

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.loginApp(key, casConnexion);
                    Thread.sleep(100);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (nemopaySession.isRegistered()) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("key", key);
                                editor.apply();

                                TextView textView = findViewById(R.id.text_app_registered);
                                if (textView != null)
                                    textView.setText(nemopaySession.getName().substring(0, nemopaySession.getName().length() - (nemopaySession.getName().matches("^.* - ([0-9]{4})([/-])([0-9]{2})\\2([0-9]{2})$") ? 13 : 0)));
                            }
                            else
                                dialog.errorDialog(BaseActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_error_registering));
                        }
                    });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(BaseActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_error_registering));
                        }
                    });
                }
            }
        }.start();
    }

    protected void setGingerKey(final String key) {
        if (key.equals(""))
            return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("key_ginger", key);
        editor.apply();

        ginger.setKey(key);
    }

    protected void setBottomatikKey(final String key) {
        if (key.equals(""))
            return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("key_bottomatik", key);
        editor.apply();

        bottomatik.setKey(key);
    }

    protected boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else
            return true;
    }

    protected boolean haveCameraPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return false;
            }
        }
        else
            return true;
    }

    protected void checkUpdate() { checkUpdate(true); }
    protected void checkUpdate(final boolean popupIfNot) {
        final Dialog updateDialog = new Dialog(BaseActivity.this);
        updateDialog.startLoading(BaseActivity.this, getString(R.string.information_collection), getString(R.string.check_update));

        new Thread() {
            @Override
            public void run() {
                HTTPRequest httpRequest = new HTTPRequest(gitUrl + manifestUrl);
                httpRequest.get();

                try {
                    final Matcher matcher = Pattern.compile("android:versionCode=\"([0-9]*)\".*android:versionName=\"(\\S*)\"").matcher(httpRequest.getResponse());
                    if (matcher.find()) {
                        if (BuildConfig.VERSION_CODE < Integer.parseInt(matcher.group(1))) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDialog.stopLoading();

                                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BaseActivity.this);
                                    alertDialogBuilder
                                        .setTitle(R.string.update)
                                        .setMessage(getString(R.string.available_update) + "\n" + getString(R.string.actual_version) + ": " + BuildConfig.VERSION_NAME + "\n" + getString(R.string.available_version) + ": " + matcher.group(2))
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.set_update, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogInterface, int id) {
                                            if (!haveStoragePermission()) {
                                                updateDialog.stopLoading();
                                                updateDialog.errorDialog(BaseActivity.this, getString(R.string.update), getString(R.string.need_storage_permission));
                                            }
                                            else if (!update(matcher.group(2))) {
                                                updateDialog.stopLoading();
                                                updateDialog.errorDialog(BaseActivity.this, getString(R.string.update), getString(R.string.can_not_update));
                                            }
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null);

                                    updateDialog.createDialog(alertDialogBuilder);
                                }
                            });
                        }
                        else
                            throw new Exception(getString(R.string.no_update));
                    }
                    else
                        throw new Exception(getString(R.string.can_not_detect_update));
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateDialog.stopLoading();

                            if (popupIfNot)
                                updateDialog.errorDialog(BaseActivity.this, getString(R.string.update), e.getMessage() + "\n" + getString(R.string.actual_version) + ": " + BuildConfig.VERSION_NAME);
                        }
                    });
                }
            }
        }.start();
    }

    protected boolean update(final String version) {
        final String destination = this.downloadLocation + getString(R.string.app_name) + " " + version + ".apk";
        final String url = this.gitUrl + getString(R.string.app_name) + " " + version + ".apk";
        final Uri uri = Uri.parse("file://" + destination);

        File file = new File(destination);
        if (file.exists())
            file.delete();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(getString(R.string.update));
        request.setTitle(getString(R.string.app_name));
        request.setDestinationUri(uri);

        final DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(uri, "application/vnd.android.package-archive");
                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(install);

                unregisterReceiver(this);
            }
        };

        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        return true;
    }
}
