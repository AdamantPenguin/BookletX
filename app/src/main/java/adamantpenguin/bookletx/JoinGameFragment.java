package adamantpenguin.bookletx;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link JoinGameFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class JoinGameFragment extends Fragment {

    private static final String ARG_GAMEID = "gameId";
    private int mGameId;

    public JoinGameFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param gameId Game ID to autofill
     * @return A new instance of fragment JoinGameFragment.
     */
    public static JoinGameFragment newInstance(int gameId) {
        JoinGameFragment fragment = new JoinGameFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_GAMEID, gameId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:  {
                // navigate to settings screen
                NavController navController = Navigation.findNavController(requireView());
                NavDirections action = JoinGameFragmentDirections.actionJoinGameFragmentToSettingsFragment();
                navController.navigate(action);
                return true;
            }
            case R.id.action_cheat_toggle: {
                // toggle cheats
                CheckBox cheatBox = requireView().findViewById(R.id.cheatModeToggler);
                if (cheatBox.getVisibility() == View.GONE) {
                    cheatBox.setVisibility(View.VISIBLE);
                } else {
                    cheatBox.setVisibility(View.GONE);
                }
                return true;
            }
            default:
                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAA");
                return super.onOptionsItemSelected(item);
        }
    }

    public void onJoinButtonClicked(View v) {
        View parent = requireView();
        String username;
        String blook;
        int gameId;
        // use try/catch to ensure fields are filled
        try {
            // get username
            EditText usernameBox = parent.findViewById(R.id.usernameEdit);
            username = usernameBox.getText().toString();
            if (username.equals("")) {throw new Exception();}
            // get game id
            EditText gameIdBox = parent.findViewById(R.id.gameIdEdit);
            gameId = Integer.parseInt(gameIdBox.getText().toString());
        } catch(Exception e) {
            Toast.makeText(requireContext(), "Invalid or missing inputs", Toast.LENGTH_LONG).show();
            return;
        }
        // get cheat mode status
        CheckBox cheatBox = parent.findViewById(R.id.cheatModeToggler);
        boolean cheatMode = cheatBox.isChecked();

        // navigate to appropriate mode
        NavController navController = Navigation.findNavController(requireView());
        NavDirections action;
        if (cheatMode) {
            action = JoinGameFragmentDirections.actionJoinGameFragmentToCheatModeFragment(
                    username, gameId, null
            );
        } else {
            // TODO legit mode
            Log.e("NYI", "Legit mode not implemented");
            return;
        }
        navController.navigate(action);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mGameId = getArguments().getInt(ARG_GAMEID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_join_game, container, false);

        Button joinButton = root.findViewById(R.id.joinGameButton);
        joinButton.setOnClickListener(this::onJoinButtonClicked);

        // add game id to text box if it is set
        if (mGameId != 0) {
            EditText idEdit = root.findViewById(R.id.gameIdEdit);
            idEdit.setText(String.valueOf(mGameId));
        }

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.join_game_menu, menu);
    }
}