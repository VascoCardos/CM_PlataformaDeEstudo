package com.veducation.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AttachedFile(
    val name: String,
    val size: Long,
    val uri: String,
    val type: String
)

class AttachedFilesAdapter(
    private val files: MutableList<AttachedFile>,
    private val onRemoveClick: (AttachedFile) -> Unit
) : RecyclerView.Adapter<AttachedFilesAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        val btnRemoveFile: ImageView = itemView.findViewById(R.id.btnRemoveFile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attached_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        
        holder.tvFileName.text = file.name
        holder.tvFileSize.text = formatFileSize(file.size)
        
        // Set appropriate icon based on file type
        when {
            file.type.startsWith("image/") -> {
                holder.ivFileIcon.setImageResource(R.drawable.ic_image)
            }
            file.type == "application/pdf" -> {
                holder.ivFileIcon.setImageResource(R.drawable.ic_file)
            }
            else -> {
                holder.ivFileIcon.setImageResource(R.drawable.ic_file)
            }
        }
        
        holder.btnRemoveFile.setOnClickListener {
            onRemoveClick(file)
        }
    }

    override fun getItemCount(): Int = files.size

    fun removeFile(file: AttachedFile) {
        val position = files.indexOf(file)
        if (position != -1) {
            files.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
