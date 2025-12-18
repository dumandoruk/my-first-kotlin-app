package com.dorukduman.artbookkotlin

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dorukduman.artbookkotlin.databinding.RecyclerRowBinding

class ArtAdapter(val artList: ArrayList<Art>) : RecyclerView.Adapter<ArtAdapter.ArtHolder>() {

    // Her bir satırın görünümünü tutan ViewHolder sınıfı
    class ArtHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder oluşturulduğunda çağrılır. Satırın layout'unu bağlar.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtHolder(binding)
    }

    // Listedeki eleman sayısını döndürür.
    override fun getItemCount(): Int {
        return artList.size
    }

    // ViewHolder'ı veri ile doldurur.
    override fun onBindViewHolder(holder: ArtHolder, position: Int) {
        // Satırdaki TextView'e sanat eseri adını yazar.
        holder.binding.recyclerViewTextView.text = artList[position].name

        // Satıra tıklandığında ne olacağını belirler.
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ArtActivity::class.java)
            // Tıklanan öğenin bir "eski" kayıt olduğunu ve ID'sini ArtActivity'ye gönderir.
            intent.putExtra("info", "old")
            intent.putExtra("id", artList[position].id)
            holder.itemView.context.startActivity(intent)
        }
    }
}
