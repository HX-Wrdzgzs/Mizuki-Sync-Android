package com.example.mizukisync

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

class B50Adapter(private val records: List<B50Record>) : RecyclerView.Adapter<B50Adapter.B50ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): B50ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_b50_card, parent, false)
        return B50ViewHolder(v)
    }

    override fun onBindViewHolder(holder: B50ViewHolder, position: Int) {
        val rec = records[position]
        holder.tvTitle.text = rec.title
        holder.tvAchieve.text = String.format("%.4f%%", rec.achievement)
        holder.tvConstant.text = "Lv ${rec.levelValue}"
        holder.tvRating.text = "Ra: ${rec.rating}"
        holder.tvGenre.text = rec.genre
        holder.tvVersion.text = rec.version

        val diffColors = arrayOf("#4CAF50", "#FBC02D", "#F44336", "#9C27B0", "#FFFFFF")
        val color = if (rec.difficulty < diffColors.size) diffColors[rec.difficulty] else "#9E9E9E"
        holder.viewDiff.setBackgroundColor(Color.parseColor(color))

        holder.tvConstant.tag = rec.difficulty

        Glide.with(holder.itemView.context)
            .load(rec.jacketUrl)
            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(4)))
            .into(holder.ivJacket)
    }

    override fun getItemCount() = records.size

    inner class B50ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivJacket: ImageView = v.findViewById(R.id.iv_b50_jacket)
        val tvTitle: TextView = v.findViewById(R.id.tv_b50_song_title)
        val tvAchieve: TextView = v.findViewById(R.id.tv_b50_achieve)
        val tvConstant: TextView = v.findViewById(R.id.tv_b50_constant)
        val tvRating: TextView = v.findViewById(R.id.tv_b50_rating)
        val tvGenre: TextView = v.findViewById(R.id.tv_b50_genre)
        val tvVersion: TextView = v.findViewById(R.id.tv_b50_version)
        val viewDiff: View = v.findViewById(R.id.view_diff_tag)
    }
}