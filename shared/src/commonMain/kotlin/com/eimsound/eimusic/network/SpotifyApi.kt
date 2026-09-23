package com.eimsound.eimusic.network

import com.eimsound.eimusic.network.models.featuredplaylist.FeaturedPlayList
import com.eimsound.eimusic.network.models.newreleases.NewReleasedAlbums
import com.eimsound.eimusic.network.models.topfiftycharts.TopFiftyCharts


/**
 * Created by abdulbasit on 26/02/2023.
 */
interface SpotifyApi {
    suspend fun getTopFiftyChart(): TopFiftyCharts
    suspend fun getNewReleases(): NewReleasedAlbums
    suspend fun getFeaturedPlaylist(): FeaturedPlayList
    suspend fun getPlayList(playlistId: String): TopFiftyCharts
}
