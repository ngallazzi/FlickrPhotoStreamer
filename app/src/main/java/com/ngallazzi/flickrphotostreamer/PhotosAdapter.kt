package com.ngallazzi.flickrphotostreamer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ngallazzi.flickrphotostreamer.repository.models.Photo
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_picture.view.*

/**
 * FlickrPhotoStreamer
 * Created by Nicola on 11/21/2018.
 * Copyright © 2018 Zehus. All rights reserved.
 */
class PhotosAdapter(private val photos: ArrayList<Photo>, private val context: Context) :
        RecyclerView.Adapter<PhotosAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_picture, parent, false))
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Picasso.get()
                .load(photos[position].getUrl())
                //.networkPolicy(NetworkPolicy.OFFLINE)
                .into(holder.ivLocation)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = photos.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivLocation: ImageView = view.ivLocation
    }
}