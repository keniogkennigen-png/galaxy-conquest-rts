package com.galaxycommand.rts.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galaxycommand.rts.R
import com.galaxycommand.rts.core.GameMap

/**
 * Adapter for displaying map cards in the map selection RecyclerView.
 */
class MapAdapter(
    private val maps: List<GameMap>,
    private val onMapClick: (GameMap) -> Unit
) : RecyclerView.Adapter<MapAdapter.MapViewHolder>() {
    
    class MapViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mapName: TextView = view.findViewById(R.id.mapName)
        val mapDescription: TextView = view.findViewById(R.id.mapDescription)
        val mapSize: TextView = view.findViewById(R.id.mapSize)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_map_card, parent, false)
        return MapViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        val map = maps[position]
        holder.mapName.text = map.name
        holder.mapDescription.text = map.description
        holder.mapSize.text = "${map.width}x${map.height}"
        
        holder.itemView.setOnClickListener {
            onMapClick(map)
        }
    }
    
    override fun getItemCount(): Int = maps.size
}
