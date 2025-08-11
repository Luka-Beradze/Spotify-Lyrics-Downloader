/*
 * =================================================================================================
 *  Spotify Lyrics Downloader
 * =================================================================================================
 *
 *  Description:
 *  This tool downloads lyrics for a given Spotify track, album, or playlist URL and saves them
 *  as .lrc files inside a single .zip archive.
 *
 *  @ Luka-Beradze
 * =================================================================================================
 */
import org.json.JSONArray;
import org.json.JSONObject;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class SpotifyDownloader {

    //--- CONFIGURE YOUR CREDENTIALS HERE ---
    private static final String SPOTIFY_CLIENT_ID = "";
    private static final String SPOTIFY_CLIENT_SECRET = "";
    // ------------------------------------------

    private static final Pattern SPOTIFY_URL_PATTERN = Pattern.compile("https?:\\/\\/open\\.spotify\\.com\\/(track|playlist|album)\\/([a-zA-Z0-9]+)");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final SpotifyApi SPOTIFY_API = new SpotifyApi.Builder()
            .setClientId(SPOTIFY_CLIENT_ID)
            .setClientSecret(SPOTIFY_CLIENT_SECRET)
            .build();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SpotifyDownloader \"<spotify_url>\"");
            System.out.println("Example: java SpotifyDownloader \"https://open.spotify.com/album/4m2880jivSbbyEGAKfITCa\"");
            return;
        }
        
        if (SPOTIFY_CLIENT_ID.equals("") || SPOTIFY_CLIENT_SECRET.equals("")) {
            System.err.println("ERROR: Please fill in your Spotify Client ID and Secret in the SpotifyDownloader.java file.");
            return;
        }

        String url = args[0];
        Matcher matcher = SPOTIFY_URL_PATTERN.matcher(url);

        if (!matcher.find()) {
            System.err.println("Invalid Spotify URL provided. Please use a valid track, album, or playlist URL.");
            return;
        }

        String type = matcher.group(1);
        String id = matcher.group(2);

        try {
            System.out.println("Authenticating with Spotify...");
            authenticateSpotify();

            switch (type) {
                case "album":
                    processAlbum(id);
                    break;
                case "playlist":
                    processPlaylist(id);
                    break;
                case "track":
                    processTrack(id);
                    break;
                default:
                    System.err.println("Unsupported URL type.");
            }
        } catch (Exception e) {
            System.err.println("\nAn error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void authenticateSpotify() throws Exception {
        var credentials = SPOTIFY_API.clientCredentials().build().execute();
        SPOTIFY_API.setAccessToken(credentials.getAccessToken());
        System.out.println("Authentication successful!");
    }
    
    private static void processAlbum(String albumId) throws Exception {
        System.out.println("Fetching album details...");
        Album album = SPOTIFY_API.getAlbum(albumId).build().execute();
        String zipFileName = sanitizeFilename(album.getName()) + ".zip";
        System.out.println("Album: " + album.getName());
        // System.out.println("Total tracks: " + album.getTotalTracks());

        List<TrackSimplified> allTracks = new ArrayList<>();
        Paging<TrackSimplified> paging;
        int offset = 0;
        do {
            paging = SPOTIFY_API.getAlbumsTracks(albumId).limit(50).offset(offset).build().execute();
            allTracks.addAll(Arrays.asList(paging.getItems()));
            offset += paging.getItems().length;
        } while (paging.getNext() != null);
        
        downloadTracksAsZip(allTracks, album.getName(), zipFileName, "album");
    }

    private static void processPlaylist(String playlistId) throws Exception {
        System.out.println("Fetching playlist details...");
        Playlist playlist = SPOTIFY_API.getPlaylist(playlistId).build().execute();
        String zipFileName = sanitizeFilename(playlist.getName()) + ".zip";
        System.out.println("Playlist: " + playlist.getName());
        System.out.println("Total tracks: " + playlist.getTracks().getTotal());

        List<Track> allTracks = new ArrayList<>();
        Paging<PlaylistTrack> paging;
        int offset = 0;
        do {
            paging = SPOTIFY_API.getPlaylistsItems(playlistId).limit(100).offset(offset).build().execute();
            for (PlaylistTrack pt : paging.getItems()) {
                if(pt.getTrack() instanceof Track) {
                   allTracks.add((Track) pt.getTrack());
                }
            }
            offset += paging.getItems().length;
        } while (paging.getNext() != null);

        downloadTracksAsZip(allTracks, playlist.getName(), zipFileName, "playlist");
    }

    private static void processTrack(String trackId) throws Exception {
        System.out.println("Fetching track details...");
        Track track = SPOTIFY_API.getTrack(trackId).build().execute();
        String zipFileName = sanitizeFilename(track.getName()) + ".zip";
        
        downloadTracksAsZip(List.of(track), track.getAlbum().getName(), zipFileName, "track");
    }

    private static <T> void downloadTracksAsZip(List<T> tracks, String collectionName, String zipFileName, String type) throws IOException {
        System.out.println("\nStarting download process...");
        int successCount = 0;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            for (int i = 0; i < tracks.size(); i++) {
                T genericTrack = tracks.get(i);
                String trackId, trackName, albumName, artistNames, duration;
                int trackNumber;

                // Handle different track types from Spotify API
                if (type.equals("album") && genericTrack instanceof TrackSimplified) {
                    TrackSimplified track = (TrackSimplified) genericTrack;
                    trackId = track.getId();
                    trackName = track.getName();
                    albumName = collectionName;
                    artistNames = Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(", "));
                    duration = formatDuration(track.getDurationMs());
                    trackNumber = track.getTrackNumber();
                } else if (genericTrack instanceof Track) {
                    Track track = (Track) genericTrack;
                    trackId = track.getId();
                    trackName = track.getName();
                    albumName = track.getAlbum().getName();
                    artistNames = Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(", "));
                    duration = formatDuration(track.getDurationMs());
                    trackNumber = track.getTrackNumber();
                } else {
                    continue; // Skip if type is not recognized
                }

                System.out.printf("[%d/%d] Fetching lyrics for: %s%n", i + 1, tracks.size(), trackName);

                LyricsResult lyricsResult = fetchLyrics(trackId);
                if (lyricsResult != null && lyricsResult.content != null) {
                    String header = String.format("[ar:%s]\n[al:%s]\n[ti:%s]\n[length:%s]\n\n",
                            artistNames, albumName, trackName, duration);

                    String lyricContent = header + lyricsResult.content;
                    String entryName = String.format("%02d. %s.lrc", trackNumber, sanitizeFilename(trackName));

                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zos.putNextEntry(zipEntry);
                    zos.write(lyricContent.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                    successCount++;
                } else {
                    System.out.println("  -> No lyrics found.");
                }
            }
        }
        
        System.out.println("\n----------------------------------------");
        if (successCount > 0) {
            System.out.printf("Success! Created %s with %d lyric file(s).%n", zipFileName, successCount);
        } else {
            System.out.println("Completed. No lyrics were found for any of the tracks.");
        }
        System.out.println("----------------------------------------");
    }

    private static LyricsResult fetchLyrics(String trackId) {
        String url = "https://spotify-lyrics-api-pi.vercel.app/?trackid=" + trackId + "&format=lrc";
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JSONObject data = new JSONObject(response.body());
            if (data.optBoolean("error", false) || data.getJSONArray("lines").isEmpty()) {
                return null;
            }

            StringBuilder lyricContent = new StringBuilder();
            boolean isSynced = !"UNSYNCED".equals(data.getString("syncType"));
            JSONArray lines = data.getJSONArray("lines");

            for (int i = 0; i < lines.length(); i++) {
                JSONObject line = lines.getJSONObject(i);
                if (isSynced) {
                    lyricContent.append("[").append(line.getString("timeTag")).append("] ");
                }
                lyricContent.append(line.getString("words")).append("\n");
            }
            return new LyricsResult(lyricContent.toString(), isSynced);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String formatDuration(int durationMs) {
        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private static String sanitizeFilename(String filename) {
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }
    
    private record LyricsResult(String content, boolean isSynced) {}
}