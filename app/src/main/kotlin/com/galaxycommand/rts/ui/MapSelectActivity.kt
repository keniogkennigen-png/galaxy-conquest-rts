package com.galaxycommand.rts.ui

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galaxycommand.rts.R
import com.galaxycommand.rts.core.MapManager
import com.galaxycommand.rts.core.GameMap

/**
 * Activity for selecting a game map before starting.
 * Displays all available maps with their previews and descriptions.
 */
class MapSelectActivity : AppCompatActivity() {
    
    private lateinit var mapList: RecyclerView
    private lateinit var adapter: MapAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_select)
        
        mapList = findViewById(R.id.mapList)
        mapList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        adapter = MapAdapter(MapManager.maps) { map ->
            onMapSelected(map)
        }
        mapList.adapter = adapter
    }
    
    private fun onMapSelected(map: GameMap) {
        // Return the selected map ID to the calling activity
        intent.putExtra("selected_map_id", map.id)
        setResult(RESULT_OK, intent)
        finish()
    }
    
    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
