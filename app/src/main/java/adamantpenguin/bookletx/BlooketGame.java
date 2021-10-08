package adamantpenguin.bookletx;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BlooketGame {
    private final int gameId;
    private String username = "";
    private String blook = "Fox"; // default value in case something goes wrong setting it later
    private String fbToken = "";
    private long balance = 0L;
    private String hackerPassword = "";
    private String stg = "join";
    private final DatabaseReference db;
    private FirebaseAuth fbAuth;

    public static final Map<String, String> glitches = new HashMap<>();
    static {
        glitches.put("j", "Jokester");
        glitches.put("lb", "Lunch Break");
        glitches.put("nt", "Night Time");
        glitches.put("as", "Ad Spam");
        glitches.put("e37", "Error 37");
        glitches.put("f", "Flip");
    }  // TODO add all glitches (if there are more)

    public static final String[] supportedGamemodeNames = {
            "inst", "fact", "hack", "gold", "def", "cafe"  // TODO add all modes
    };

    /**
     * Blooket game interaction helper class thing.
     * Use it to talk to Blooket live games without fuddling with Firebase.
     * @param context requireContext() or something
     * @param gameId Game ID of the live game
     */
    public BlooketGame(Context context, int gameId) {
        this.gameId = gameId;

        // get rid of any existing Firebase
        try {FirebaseApp.getInstance().delete();} catch (IllegalStateException ignored) {}
        // set up Firebase
        String[] urls = {
                "https://blooket-2020.firebaseio.com",
                "https://blooket-2021.firebaseio.com",
                "https://blooket-2022.firebaseio.com",
                "https://blooket-2023.firebaseio.com",
                "https://blooket-2024.firebaseio.com",
                "https://blooket-2025.firebaseio.com",
                "https://blooket-2026.firebaseio.com",
                "https://blooket-2027.firebaseio.com"
        };
        String correctUrl =  // blooket uses different urls based on the game id
                this.gameId < 212500 ? urls[0] :
                this.gameId < 325000 ? urls[1] :
                this.gameId < 437500 ? urls[2] :
                this.gameId < 550000 ? urls[3] :
                this.gameId < 662500 ? urls[4] :
                this.gameId < 775000 ? urls[5] :
                this.gameId < 887500 ? urls[6] :
                urls[7];
        FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()  // data here taken from blooket.com's JS
                .setApiKey("AIzaSyCA-cTOnX19f6LFnDVVsHXya3k6ByP_MnU")
                .setApplicationId("1:741533559105:web:b8cbb10e6123f2913519c0")
                .setProjectId("blooket-2020")
                .setStorageBucket("blooket-2020.appspot.com")
                .setGcmSenderId("741533559105")
                .setDatabaseUrl(correctUrl)
                .build());

        this.db = FirebaseDatabase.getInstance().getReference().child(String.valueOf(gameId)).getRef();

        // add event listener for stg changes
        this.db.child("stg").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                String value = (String) snapshot.getValue();
                if (value != null) stg = value;
                if (!stg.equals("join") && !stg.equals("fin") && !stg.equals("inst")) {
                    // once game started, register balance updater
                    db.child("c").child(username).child(balanceKeyName(stg)).addValueEventListener(balanceUpdater);
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error)  {
                 Log.w("DbError", error.toException());
            }
        });
    }

    public int getGameId() { return this.gameId; }
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }
    public String getBlook() { return this.blook; }
    public void setBlook(String blook) { this.blook = blook; }
    public boolean updateBlook(String blook) {
        try {
            this.blook = blook;
            this.db.child("c").child(this.username).child("b").setValue(blook);
            return true;
        } catch (Exception ignored) {}
        return false;
    }
    public void setToken(String fbToken) {
        this.fbToken = fbToken;
        this.fbAuth = FirebaseAuth.getInstance();
        this.fbAuth.signInWithCustomToken(this.fbToken);
    }
    public String getHackerPassword() { return this.hackerPassword; }
    public boolean setHackerPassword(String password) {
        if (this.stg.equals("hack")) {  // don't do anything if wrong gamemode
            try {
                this.hackerPassword = password;
                this.db.child("c").child(this.username).child("p").setValue(password);
                return true;
            } catch (Exception ignored) {}
        }
        return false;  // return success or not
    }
    public void sendAnswerStats() {
        // TODO allow giving real stats as parameter(s)
        HashMap<String, Integer> corrects = new HashMap<>();
        corrects.put("1", 10);  // says i got question 1 correct 10 times

        HashMap<String, Integer> incorrects = new HashMap<>();
        incorrects.put("2", 1);  // says i got question 2 wrong once

        this.db.child("c").child(this.username).child("c").setValue(corrects);
        this.db.child("c").child(this.username).child("i").setValue(incorrects);
    }

    /**
     * Authenticate with Blooket API (api.blooket.com).
     * This allows access to writing to the database - i.e. playing the game.
     * @param username Username/nickname
     */
    public void authenticate(String username, Callback callback) {
        this.username = username;
        // send request starts here
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        String data = String.format( // what goes to server
                Locale.ENGLISH,
                "{\"id\":\"%d\",\"name\":\"%s\"}",
                this.gameId, this.username);
        Log.d("BlAuth", data);
        RequestBody body = RequestBody.create(data, JSON);

        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                         + "QtWebEngine/5.15.2 Chrome/83.0.4103.122 Safari/537.36";  // pretend to be a web browser
        Request request = new Request.Builder()
                .url("https://api.blooket.com/api/firebase/join")
                .addHeader("Referer", "https://www.blooket.com/")
                .addHeader("User-Agent", userAgent)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .put(body)
                .build();
        // use the callback from the caller
        client.newCall(request).enqueue(callback);
    }

    private String balanceKeyName(String stg) {
        switch (stg) {
            case "hack": return "c";
            case "fact": case "cafe": return "ca";
            case "gold": return "g";
            case "def": return "d";
            default: return "";
        }
    }


    /**
     * Do something when the 'stg' value (usually represents gamemode) changes
     * @param listener ValueEventListener for what to do
     */
    public void onStgChanged(ValueEventListener listener) {
        this.db.child("stg").addValueEventListener(listener);
    }

    /**
     * Do something when a given user's balance changes
     * @param targetUsername the user to monitor
     * @param listener ValueEventListener for what to do
     */
    public void onBalanceChanged(String targetUsername, ValueEventListener listener) {
        this.db.child("c").child(targetUsername).child(balanceKeyName(this.stg)).addValueEventListener(listener);
    }

    /**
     * Do something when the players change (e.g. if someone joins)
     * @param listener ValueEventListener for what to do
     */
    public void onPlayersChanged(ValueEventListener listener) { this.db.child("c").addValueEventListener(listener); }

    // player-related methods
    public void enterGame() {
        HashMap<String, String> data = new HashMap<>();
        data.put("b", this.blook);
        this.db.child("c").child(this.username).setValue(data);
    }
    public void exitGame() { this.db.child("c").child(this.username).removeValue(); }

    // balance-related methods
    public void setBalance(long amount) {
        this.db.child("c").child(this.username).child(balanceKeyName(stg)).setValue(amount);
    }
    ValueEventListener balanceUpdater = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
            try {
                balance = (Long) snapshot.getValue();
            } catch (Exception ignored) {}
        }

        @Override
        public void onCancelled(@NonNull @NotNull DatabaseError error) {
            Log.w("DbError", error.toException());
        }
    };
    public long getBalance() { return this.balance; }

    // glitch/hack/etc-related methods
    public void doGlitch(String glitchCode) {
        this.db.child("c").child(username).child("tat").setValue(glitchCode);
    }
    public static String glitchNameToGlitchCode(String glitchName) {
        if (glitches.containsValue(glitchName)) {
            for (Map.Entry<String, String> checkEntry : glitches.entrySet()) {
                if (checkEntry.getValue().equals(glitchName)) {
                    return checkEntry.getKey();
                }
            }
        }
        return "";
    }
    public void hackPlayer(String targetUsername, long amount) {
        this.db.child("c").child(this.username).child("tat").setValue(
                targetUsername + ":" + amount
        );
        this.setBalance(this.balance + amount);
    }
    public void stealGold(String targetUsername, long amount, boolean swapMode) {
        this.db.child("c").child(this.username).child("tat").setValue(
                targetUsername + (swapMode ? ":swap:" : ":") + amount
        );
        this.setBalance((swapMode ? 0 : this.balance) + amount);
    }

    // TODO add other methods for specific gamemodes
}
