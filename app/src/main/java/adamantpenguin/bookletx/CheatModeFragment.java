package adamantpenguin.bookletx;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CheatModeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CheatModeFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_GAMEID = "gameId";

    private String mUsername;
    private int mGameId;
    private BlooketGame game;

    public CheatModeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param username Username/nickname
     * @param gameId Game ID
     * @return A new instance of fragment CheatModeFragment.
     */
    public static CheatModeFragment newInstance(String username, int gameId) {
        CheatModeFragment fragment = new CheatModeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putInt(ARG_GAMEID, gameId);
        fragment.setArguments(args);
        return fragment;
    }

    // from https://stackoverflow.com/a/16262479
    private static ArrayList<View> getViewsByTag(ViewGroup root, String tag) {
        ArrayList<View> views = new ArrayList<>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getViewsByTag((ViewGroup) child, tag));
            }

            final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(tag)) {
                views.add(child);
            }

        }
        return views;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUsername = getArguments().getString(ARG_USERNAME);
            mGameId = getArguments().getInt(ARG_GAMEID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_cheat_mode, container, false);

        // hide all the gamemode-specific stuff
        ArrayList<View> hideMe = getViewsByTag((ViewGroup) root, "allMode");
        for (View view : hideMe) { view.setVisibility(View.GONE); }
        for (String name : BlooketGame.supportedGamemodeNames) {
            hideMe = getViewsByTag((ViewGroup) root, name + "Mode");
            for (View view : hideMe) { view.setVisibility(View.GONE); }
        }

        // connect to game
        this.game = new BlooketGame(requireContext(), mGameId);
        this.game.authenticate(mUsername, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("BlAuth", "request failed for some reason");
                TextView statusText = root.findViewById(R.id.statusText);
                statusText.setText(R.string.conn_failed);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JSONObject responseJson;
                try {
                    responseJson = new JSONObject(Objects.requireNonNull(response.body()).string());
                } catch (JSONException e) {
                    Log.e("JsonParse", e.getMessage());
                    responseJson = new JSONObject();
                }
                // update fbToken
                try {
                    game.setToken(responseJson.getString("fbToken"));
                } catch (JSONException e) {
                    Log.e("BlAuth", "Invalid API request");
                    int problemText;

                    String problemString;
                    try {problemString = responseJson.getString("msg");}
                    catch (JSONException ignored) {problemString = "unknown";}
                    Log.d("BlAuth", problemString);

                    switch (problemString) {
                        case "no game": {
                            problemText = R.string.error_no_game;
                            break;
                        }
                        case "taken": {
                            problemText = R.string.error_name_taken;
                            break;
                        }
                        default: {
                            problemText = R.string.error_unknown;
                            Log.e("BlAuth", problemString);
                            break;
                        }
                    }
                    int finalProblemText = problemText;
                    requireActivity().runOnUiThread(() -> {
                                TextView statusText = root.findViewById(R.id.statusText);
                                statusText.setText(finalProblemText);
                    });
                    return;
                }

                // update blook if possible
                try {
                    game.setBlook(responseJson.getString("blook"));
                } catch (JSONException ignored) {}

                // enable connection-based views
                requireActivity().runOnUiThread(() -> {
                    TextView statusText = root.findViewById(R.id.statusText);
                    statusText.setText(R.string.conn_success);
                    ArrayList<View> buttons = getViewsByTag((ViewGroup) root, "connected");
                    for (View button : buttons) { button.setEnabled(true); }
                });

                // event handler for gamemode changes, to show different buttons
                game.onStgChanged(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        String value = (String) snapshot.getValue();
                        TextView statusText = root.findViewById(R.id.statusText);

                        if (value == null) {  // database is deleted! disable buttons
                            statusText.setText(R.string.conn_gone);
                            ArrayList<View> buttons = getViewsByTag((ViewGroup) root, "connected");
                            for (View button : buttons) { button.setEnabled(false); }
                            return;  // also prevent later NPEs
                        }

                        boolean isGameStarted = !value.equals("join") && !value.equals("fin") && !value.equals("inst");

                        // hide everything first
                        for (String name : BlooketGame.supportedGamemodeNames) {
                            ArrayList<View> hideMe = getViewsByTag((ViewGroup) root, name + "Mode");
                            for (View view : hideMe) { view.setVisibility(View.GONE); }
                        }

                        // show allMode things if it's not join/fin/inst, otherwise hide
                        ArrayList<View> allModeTagged = getViewsByTag((ViewGroup) root, "allMode");
                        if (isGameStarted) {
                            for (View view : allModeTagged) { view.setVisibility(View.VISIBLE); }
                        } else {
                            for (View view : allModeTagged) { view.setVisibility(View.GONE); }
                            if (value.equals("fin")) {
                                statusText.setText(R.string.game_over);
                                // TODO send bogus question answer stats
                            }
                        }

                        // show items for gamemode
                        ArrayList<View> showMe = getViewsByTag((ViewGroup) root, value + "Mode");
                        for (View view : showMe) { view.setVisibility(View.VISIBLE); }


                        // also register balance self-monitor-er™ once game is started
                        if (isGameStarted) {
                            game.onBalanceChanged(game.getUsername(), new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                                    if (snapshot.getValue() != null) {
                                        long value = (long) snapshot.getValue();
                                        String formattedBalance = balanceFormatter.format(value);
                                        statusText.setText(String.format(
                                                Locale.ENGLISH,
                                                "%s: %s",
                                                getString(R.string.balance_indicator_status), formattedBalance
                                        ));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull @NotNull DatabaseError error) {}
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {}
                });

                // when someone joins or leaves
                game.onPlayersChanged(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        // don't run with no players, to avoid screwing up the layout
                        if (snapshot.getChildrenCount() > 0) {
                            Iterable<DataSnapshot> children = snapshot.getChildren();
                            ArrayList<String> players = new ArrayList<>();
                            for (DataSnapshot child : children) {
                                players.add(child.getKey());
                            }

                            // update spinners for player choosing
                            Spinner[] updateMe = {
                                    root.findViewById(R.id.hackPlayerChooser),
                                    root.findViewById(R.id.stealPlayerChooser)
                            };

                            for (Spinner spinner : updateMe) {
                                spinner.setAdapter(new ArrayAdapter<>(
                                        root.getContext(),
                                        android.R.layout.simple_spinner_dropdown_item,
                                        players
                                ));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {}
                });
            }
        });


        // button onClicks

        Button enterButton = root.findViewById(R.id.enterGameButton);
        enterButton.setOnClickListener(this::onEnterButtonClicked);

        Button exitButton = root.findViewById(R.id.exitGameButton);
        exitButton.setOnClickListener(this::onExitButtonClicked);

        Button balanceButton = root.findViewById(R.id.updateBalanceButton);
        balanceButton.setOnClickListener(this::onBalanceUpdateButtonClicked);

        Button glitchButton = root.findViewById(R.id.glitchButton);
        glitchButton.setOnClickListener(this::onGlitchButtonClicked);

        Button setPasswordButton = root.findViewById(R.id.setPasswordButton);
        setPasswordButton.setOnClickListener(this::onSetPasswordButtonClicked);

        Button kickPlayerButton = root.findViewById(R.id.kickPlayerButton);
        kickPlayerButton.setOnClickListener(this::onKickPlayerButtonClicked);

        Button hackButton = root.findViewById(R.id.hackButton);
        hackButton.setOnClickListener(this::onHackButtonClicked);

        Button stealButton = root.findViewById(R.id.stealButton);
        stealButton.setOnClickListener(this::onStealButtonClicked);

        return root;
    }

    public void onEnterButtonClicked(View v) { this.game.enterGame(); }
    public void onExitButtonClicked(View v) { this.game.exitGame(); }
    public void onKickPlayerButtonClicked(View v) {
        Spinner playerChooser = requireView().findViewById(R.id.kickPlayerChooser);
        String targetUsername = (String) playerChooser.getSelectedItem();
        this.game.kickPlayer(targetUsername);
    }
    public void onBalanceUpdateButtonClicked(View v) {
        EditText editBalance = requireView().findViewById(R.id.editBalance);
        try {
            int newBalance = Integer.parseInt(String.valueOf(editBalance.getText()));
            this.game.setBalance(newBalance);
            editBalance.setText("");
        } catch (Exception ignored) {}
    }
    public void onGlitchButtonClicked(View v) {
        Spinner glitchChooser = requireView().findViewById(R.id.glitchChooser);
        int glitchId = glitchChooser.getSelectedItemPosition();
        BlooketGame.Glitch[] glitches = {
                BlooketGame.Glitch.AD_SPAM,
                BlooketGame.Glitch.LUNCH_BREAK,
                BlooketGame.Glitch.NIGHT_TIME,
                BlooketGame.Glitch.JOKESTER
        };
        BlooketGame.Glitch glitch = glitches[glitchId];
        this.game.doGlitch(glitch);
    }
    public void onSetPasswordButtonClicked(View v) {
        EditText editPassword = requireView().findViewById(R.id.editHackerPassword);
        if (this.game.setHackerPassword(editPassword.getText().toString())) {
            /*
            // if it worked, remove box since password should only be set once
            editPassword.setVisibility(View.GONE);
            v.setVisibility(View.GONE);*/

            // if it worked, clear box (don't remove because why not :D)
            editPassword.setText("");
        }
    }
    public void onHackButtonClicked(View v) {
        Spinner playerChooser = requireView().findViewById(R.id.hackPlayerChooser);
        String targetUsername = (String) playerChooser.getSelectedItem();

        EditText editAmount = requireView().findViewById(R.id.editHackAmount);
        try {
            long hackAmount = Long.parseLong(String.valueOf(editAmount.getText()));
            this.game.hackPlayer(targetUsername, hackAmount);
            editAmount.setText("");
        } catch (Exception ignored) {}
    }
    public void onStealButtonClicked(View v) {
        Spinner playerChooser = requireView().findViewById(R.id.stealPlayerChooser);
        String targetUsername = (String) playerChooser.getSelectedItem();

        EditText editAmount = requireView().findViewById(R.id.editStealAmount);
        try {
            long stealAmount = Long.parseLong(String.valueOf(editAmount.getText()));
            this.game.stealGold(targetUsername, stealAmount, false);
            editAmount.setText("");
        } catch (Exception ignored) {}
    }

    // balance formatting to make it look nice
    private final DecimalFormat balanceFormatter = new DecimalFormat("$###,###");
}