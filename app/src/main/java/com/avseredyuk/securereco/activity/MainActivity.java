package com.avseredyuk.securereco.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.avseredyuk.securereco.R;
import com.avseredyuk.securereco.adapter.CallArrayAdapter;
import com.avseredyuk.securereco.application.Application;
import com.avseredyuk.securereco.auth.AuthenticationManager;
import com.avseredyuk.securereco.dao.CallDao;
import com.avseredyuk.securereco.exception.AuthenticationException;
import com.avseredyuk.securereco.model.Call;
import com.avseredyuk.securereco.util.ConfigUtil;

import java.io.File;
import java.util.List;

import static com.avseredyuk.securereco.util.Constant.IS_ENABLED;

public class MainActivity extends AppCompatActivity {
    private CallArrayAdapter callArrayAdapter;

    @Override
    protected void onResume() {
        super.onResume();
        ListView callsListView = (ListView) findViewById(R.id.listView);
        List<Call>  calls = CallDao.getInstance().findAll(Call.CallDateComparator);
        callArrayAdapter = new CallArrayAdapter(this, calls);
        callsListView.setAdapter(callArrayAdapter);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            int color;
            if (((Application) getApplicationContext()).isAuthenticated()) {
                color = R.color.colorAuthenticated;
            } else {
                color = R.color.colorPrimary;
            }
            actionBar.setBackgroundDrawable(
                    new ColorDrawable(
                            getResources().getColor(color)));
        }

        System.out.println("RESUMED");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteSelectedMenuItem = menu.findItem(R.id.action_delete_selected);
        MenuItem enabledDisabledMenuItem = menu.findItem(R.id.action_on_off);

        enabledDisabledMenuItem.setChecked(ConfigUtil.readBoolean(IS_ENABLED));
        int selectedCount = callArrayAdapter.getCheckedCount();
        String itemTitle;
        if (selectedCount == 0) {
            deleteSelectedMenuItem.setVisible(false);
        } else {
            itemTitle = getString(R.string.menu_item_delete_selected) + " (" + selectedCount + ")";
            deleteSelectedMenuItem.setTitle(itemTitle);
            deleteSelectedMenuItem.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    //todo: refactor this trash
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_authenticate:
                LayoutInflater li = LayoutInflater.from(this);
                View promptsView = li.inflate(R.layout.password_prompt, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setView(promptsView);
                final EditText userInput = (EditText) promptsView
                        .findViewById(R.id.editTextDialogUserInput);
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        String password = userInput.getText().toString();
                                        try {
                                            AuthenticationManager authMan = new AuthenticationManager();
                                            authMan.authenticate(password);
                                            ((Application) getApplicationContext()).setAuthMan(authMan);

                                            ActionBar actionBar = getSupportActionBar();
                                            if (actionBar != null) {
                                                actionBar.setBackgroundDrawable(
                                                        new ColorDrawable(
                                                                getResources().getColor(R.color.colorAuthenticated)));
                                            }

                                            Toast.makeText(getApplication(), "Authenticated", Toast.LENGTH_SHORT).show();
                                        } catch (AuthenticationException e) {
                                            Toast.makeText(getApplication(), "Error", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;

            case R.id.action_show_settings:
                Intent settingActivityIntent = new Intent(this, SettingsActivity.class);
//                settingActivityIntent.putExtra("auth", ((Application) getApplicationContext()).getAuthMan());
                startActivity(settingActivityIntent);
                return true;

            case R.id.action_on_off:
                Boolean isEnabledPrevious = ConfigUtil.readBoolean(IS_ENABLED);
                Boolean isEnabledNew = !isEnabledPrevious;
                ConfigUtil.writeValue(IS_ENABLED, isEnabledNew.toString().toLowerCase());
                return true;

            case R.id.action_delete_selected:
                String toastText;
                if (callArrayAdapter.getCheckedCount() > 0) {
                    List<Integer> checkedIndexes = callArrayAdapter.getCheckedStatuses();
                    for (Integer i : checkedIndexes) {
                        Call call = callArrayAdapter.getItem(i);
                        File file = new File(call.getFilename());
                        if (file.delete()) {
                            callArrayAdapter.remove(call);
                        }
                    }
                    callArrayAdapter.resetCheckedItems();
                    toastText = "Records deleted";
                } else {
                    toastText = "Nothing to delete";
                }
                Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        System.out.println("CREATED");
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((Application) getApplicationContext()).setAuthMan(null);
        //todo clear key from memory
        System.out.println("PAUSED");
    }

    @Override
    protected void onStop() {
        super.onStop();
        ((Application) getApplicationContext()).setAuthMan(null);
        //todo clear key from memory
        System.out.println("STOPPED");
    }
}
