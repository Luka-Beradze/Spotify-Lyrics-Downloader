# Spotify Lyrics Downloader

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-JDK 11+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
</p>

A simple, command-line tool for downloading lyrics for a Spotify track, album, or playlist. The application fetches the tracklist and saves all available lyrics as individual `.lrc` files within a single `.zip` archive.

## Features
- Supports tracks, albums, and playlists.
- Downloads synced lyrics in the standard `.lrc` format.
- Packages all lyric files into a convenient `.zip` archive named after the album or playlist.
- Runs on any major OS (Windows, macOS, Linux).

## Prerequisites
- **Java Development Kit (JDK) 11 or newer.** You can verify your installation by running `java -version`.
- **Included Libraries.** The `lib` folder containing all necessary `.jar` files must be in the same directory as the application.

---

## How to Use

### Part 1: Get Spotify API Credentials
To use the Spotify API, you first need to register a free "app" on their developer dashboard.

1.  Navigate to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and log in.
2.  Click the green **"Create app"** button.
3.  Fill out the form:
    *   **App name:** Can be anything (e.g., 'MyLyricsDownloader').
    *   **App description:** Can be anything (e.g., 'Test').
    *   **Redirect URI:** This field is **not used** by our console application. You must enter a valid placeholder URL. A standard one is `http://localhost`.
4.  Agree to the terms and click **"Save"**.
5.  On your new app's dashboard, you will see your **Client ID**. Click **"Show client secret"** to see your **Client Secret**.
6.  Keep these two values handy for the next step.

### Part 2: Configure the Application
1.  Open the `SpotifyDownloader.java` file in any text editor.
2.  Paste your **Client ID** and **Client Secret** into the placeholder fields around line 38.
    **Example:**
    ```java
    // --- STEP 1: CONFIGURE YOUR CREDENTIALS ---
    private static final String SPOTIFY_CLIENT_ID = "a1b2c3d4e5f67890a1b2c3d4e5f67890";
    private static final String SPOTIFY_CLIENT_SECRET = "x1y2z3a4b5c67890x1y2z3a4b5c67890";
    // ------------------------------------------
    ```
3.  Save the file.

### Part 3: Compile and Run

#### 1. Compile the Code
Open a terminal or command prompt in the project's root directory (the one containing `SpotifyDownloader.java` and the `lib` folder). Run the following command to compile the source code into a runnable Java class.

*   On **Windows**:
    ```powershell
    javac -cp ".;lib/*" SpotifyDownloader.java
    ```
*   On **macOS/Linux**:
    ```bash
    javac -cp ".:lib/*" SpotifyDownloader.java
    ```

This will create a new file: `SpotifyDownloader.class`.

#### 2. Run the Program
Now, execute the compiled class, passing the Spotify URL as an argument.

*   On **Windows**:
    ```powershell
    java -cp ".;lib/*" SpotifyDownloader "YOUR_SPOTIFY_URL_HERE"
    ```
*   On **macOS/Linux**:
    ```bash
    java -cp ".:lib/*" SpotifyDownloader "YOUR_SPOTIFY_URL_HERE"
    ```

**Example Usage:**
```bash
# Downloads lyrics for a single track
java -cp ".;lib/*" SpotifyDownloader "https://open.spotify.com/track/5pPPd1mWUBZLejws9xf5Sp"

# Downloads all lyrics from an album
java -cp ".;lib/*" SpotifyDownloader "https://open.spotify.com/album/4m2880jivSbbyEGAKfITCa"
```

A `.zip` file will be created in the same directory.

---

## Troubleshooting

-   **`Error: Could not find or load main class SpotifyDownloader`**
    This means either:
    1.  You haven't compiled the code yet (run the `javac` command).
    2.  You are using the wrong classpath separator for your OS. Remember to use a semicolon (`;`) for Windows and a colon (`:`) for macOS/Linux.

-   **`Exception in thread "main" java.lang.NoClassDefFoundError: ...`**
    This error means a required library is missing. Make sure all `.jar` files are inside the `lib` folder and that the `lib` folder is in the same directory as `SpotifyDownloader.java`.

-   **`ERROR: Please fill in your Spotify Client ID and Secret...`**
    This means you have added your credentials to the `.java` file but have not **re-compiled** it. Run the `javac` command again to include your new credentials in the `.class` file.
