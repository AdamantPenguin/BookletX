package adamantpenguin.bookletx;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CheatModeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CheatModeFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_GAMEID = "gameId";
    private static final String ARG_BLOOK = "blook";

    private String mUsername;
    private int mGameId;
    private String mBlook;
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
     * @param blook Blook
     * @return A new instance of fragment CheatModeFragment.
     */
    public static CheatModeFragment newInstance(String username, int gameId, String blook) {
        CheatModeFragment fragment = new CheatModeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putInt(ARG_GAMEID, gameId);
        args.putString(ARG_BLOOK, blook);
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
            mBlook = getArguments().getString(ARG_BLOOK);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_cheat_mode, container, false);

        this.game = new BlooketGame(requireContext(), mGameId);
        this.game.authenticate(mUsername, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("BlAuth", "request failed for some reason");
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

                requireActivity().runOnUiThread(() -> {
                    TextView statusText = root.findViewById(R.id.statusText);
                    statusText.setText(R.string.conn_success);
                    ArrayList<View> buttons = getViewsByTag((ViewGroup) root, "connDependent");
                    for (View button : buttons) {
                        button.setEnabled(true);
                    }
                });
            }
        });

        Button enterButton = root.findViewById(R.id.enterGameButton);
        enterButton.setOnClickListener(this::onEnterButtonClicked);

        Button exitButton = root.findViewById(R.id.exitGameButton);
        exitButton.setOnClickListener(this::onExitButtonClicked);

        Button balanceButton = root.findViewById(R.id.updateBalanceButton);
        balanceButton.setOnClickListener(this::onBalanceUpdateButtonClicked);

        Button glitchButton = root.findViewById(R.id.glitchButton);
        glitchButton.setOnClickListener(this::onGlitchButtonClicked);

        return root;
    }

    public void onEnterButtonClicked(View v) { this.game.enterGame(); }
    public void onExitButtonClicked(View v) { this.game.exitGame(); }
    public void onBalanceUpdateButtonClicked(View v) {
        EditText editBalance = requireView().findViewById(R.id.editBalance);
        try {
            int newBalance = Integer.parseInt(editBalance.getText().toString());
            this.game.setBalance(newBalance);
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
}