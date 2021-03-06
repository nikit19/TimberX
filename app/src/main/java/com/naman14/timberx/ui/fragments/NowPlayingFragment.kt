/*
 * Copyright (c) 2019 Naman Dwivedi.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.naman14.timberx.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ONE
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_NONE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.naman14.timberx.R
import com.naman14.timberx.databinding.FragmentNowPlayingBinding
import com.naman14.timberx.extensions.addFragment
import com.naman14.timberx.extensions.inflateWithBinding
import com.naman14.timberx.extensions.observe
import com.naman14.timberx.extensions.safeActivity
import com.naman14.timberx.models.QueueData
import com.naman14.timberx.repository.SongsRepository
import com.naman14.timberx.ui.bindings.setImageUrl
import com.naman14.timberx.ui.fragments.base.BaseNowPlayingFragment
import com.naman14.timberx.util.AutoClearedValue
import kotlinx.android.synthetic.main.fragment_now_playing.btnBack
import kotlinx.android.synthetic.main.fragment_now_playing.btnLyrics
import kotlinx.android.synthetic.main.fragment_now_playing.btnNext
import kotlinx.android.synthetic.main.fragment_now_playing.btnPrevious
import kotlinx.android.synthetic.main.fragment_now_playing.btnQueue
import kotlinx.android.synthetic.main.fragment_now_playing.btnRepeat
import kotlinx.android.synthetic.main.fragment_now_playing.btnShuffle
import kotlinx.android.synthetic.main.fragment_now_playing.btnTogglePlayPause
import kotlinx.android.synthetic.main.fragment_now_playing.progressText
import kotlinx.android.synthetic.main.fragment_now_playing.seekBar
import kotlinx.android.synthetic.main.fragment_now_playing.songTitle
import kotlinx.android.synthetic.main.fragment_now_playing.upNextAlbumArt
import kotlinx.android.synthetic.main.fragment_now_playing.upNextArtist
import kotlinx.android.synthetic.main.fragment_now_playing.upNextTitle

class NowPlayingFragment : BaseNowPlayingFragment() {
    var binding by AutoClearedValue<FragmentNowPlayingBinding>(this)
    private var queueData: QueueData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflater.inflateWithBinding(R.layout.fragment_now_playing, container)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.let {
            it.viewModel = nowPlayingViewModel
            it.setLifecycleOwner(this)

            nowPlayingViewModel.currentData.observe(this) { setNextData() }
            nowPlayingViewModel.queueData.observe(this) { queueData ->
                this.queueData = queueData
                setNextData()
            }
        }
        setupUI()
    }

    //TODO this should not here, move it to BindingAdapter or create a separate queue view model
    private fun setNextData() {
        val queue = queueData?.queue ?: return
        if (queue.isNotEmpty() && nowPlayingViewModel.currentData.value != null) {

            val currentIndex = queue.indexOf(nowPlayingViewModel.currentData.value!!.mediaId!!.toLong())
            if (currentIndex + 1 < queue.size) {
                val nextSong = SongsRepository.getSongForId(safeActivity, queue[currentIndex + 1])
                setImageUrl(upNextAlbumArt, nextSong.albumId)
                upNextTitle.text = nextSong.title
                upNextArtist.text = nextSong.artist
            } else {
                //nothing up next, show same
                upNextAlbumArt.setImageResource(R.drawable.ic_music_note)
                upNextTitle.text = getString(R.string.queue_ended)
                upNextArtist.text = getString(R.string.no_song_next)
            }
        }
    }

    private fun setupUI() {
        songTitle.isSelected = true
        btnTogglePlayPause.setOnClickListener {
            nowPlayingViewModel.currentData.value?.let { mediaData ->
                mainViewModel.mediaItemClicked(mediaData.toDummySong(), null)
            }
        }
        btnNext.setOnClickListener {
            mainViewModel.transportControls().skipToNext()
        }
        btnPrevious.setOnClickListener {
            mainViewModel.transportControls().skipToPrevious()
        }

        btnRepeat.setOnClickListener {
            when (nowPlayingViewModel.currentData.value?.repeatMode) {
                REPEAT_MODE_NONE -> mainViewModel.transportControls().setRepeatMode(REPEAT_MODE_ONE)
                REPEAT_MODE_ONE -> mainViewModel.transportControls().setRepeatMode(REPEAT_MODE_ALL)
                REPEAT_MODE_ALL -> mainViewModel.transportControls().setRepeatMode(REPEAT_MODE_NONE)
            }
        }
        btnShuffle.setOnClickListener {
            when (nowPlayingViewModel.currentData.value?.shuffleMode) {
                SHUFFLE_MODE_NONE -> mainViewModel.transportControls().setShuffleMode(SHUFFLE_MODE_ALL)
                SHUFFLE_MODE_ALL -> mainViewModel.transportControls().setShuffleMode(SHUFFLE_MODE_NONE)
            }
        }

        btnQueue.setOnClickListener { safeActivity.addFragment(fragment = QueueFragment()) }
        btnBack.setOnClickListener { safeActivity.onBackPressed() }

        buildUIControls()
    }

    private fun buildUIControls() {
        mainViewModel.mediaController.observe(this) { mediaController ->
            progressText.setMediaController(mediaController)
            seekBar.setMediaController(mediaController)
        }

        btnLyrics.setOnClickListener {
            val currentSong = nowPlayingViewModel.currentData.value
            val artist = currentSong?.artist
            val title = currentSong?.title
            if (artist != null && title != null) {
                safeActivity.addFragment(fragment = LyricsFragment.newInstance(artist, title))
            }
        }
    }

    override fun onStop() {
        progressText.disconnectController()
        seekBar.disconnectController()
        super.onStop()
    }
}
