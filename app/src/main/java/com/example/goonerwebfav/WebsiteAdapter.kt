package com.example.goonerwebfav

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.URL

class WebsiteAdapter(
    private var items: List<WebsiteEntity> = emptyList(),
    private val isSelectMode: Boolean = false,
    private val selectedUrls: Set<String> = emptySet()
) : RecyclerView.Adapter<WebsiteAdapter.ViewHolder>() {

    var onItemClick: ((WebsiteEntity) -> Unit)? = null
    var onItemLongClick: ((View, WebsiteEntity) -> Unit)? = null
    var onCheckChanged: ((WebsiteEntity, Boolean) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val ivFavicon: ImageView = view.findViewById(R.id.ivFavicon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvUrl: TextView = view.findViewById(R.id.tvUrl)
        val layoutTags: LinearLayout = view.findViewById(R.id.layoutTags)
        val tvNote: TextView = view.findViewById(R.id.tvNote)
        val tvVisitCount: TextView = view.findViewById(R.id.tvVisitCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_website, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 批量选择模式
        if (isSelectMode) {
            holder.cbSelect.visibility = View.VISIBLE
        } else {
            holder.cbSelect.visibility = View.GONE
        }

        holder.cbSelect.isChecked = selectedUrls.contains(item.url)

        holder.cbSelect.setOnClickListener {
            onCheckChanged?.invoke(item, holder.cbSelect.isChecked)
        }

        // 标题展示
        val title = if (item.title.isNotBlank()) {
            item.title
        } else {
            try {
                java.net.URI(item.url).host ?: item.url
            } catch (e: Exception) {
                item.url
            }
        }
        holder.tvTitle.text = title

        // 域名展示
        try {
            holder.tvUrl.text = java.net.URI(item.url).host ?: item.url
        } catch (e: Exception) {
            holder.tvUrl.text = item.url
        }

        // 异步加载网站图标 (Favicon)
        holder.ivFavicon.setImageDrawable(null)
        holder.ivFavicon.setTag(item.url)
        if (item.url.startsWith("http")) {
            Thread {
                try {
                    val host = java.net.URI(item.url).host
                    val faviconUrl = "https://$host/favicon.ico"
                    val stream = URL(faviconUrl).openStream()
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        Handler(Looper.getMainLooper()).post {
                            if (holder.ivFavicon.tag == item.url) {
                                holder.ivFavicon.setImageBitmap(bitmap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 获取失败就保持空白
                }
            }.start()
        }

        // 备注展示
        if (item.note.isNotBlank()) {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = item.note
        } else {
            holder.tvNote.visibility = View.GONE
        }

        // 👇 【核心逻辑】只有排在第一位（上一次访问的）且访问过的，才高亮显示 👇
        val isLastVisited = (position == 0 && item.visitCount > 0)

        if (isLastVisited) {
            // 上一次访问的高亮逻辑
            holder.tvVisitCount.text = "上次访问"
            holder.tvVisitCount.setTextColor(0xFFFF66CC.toInt()) // 粉色
            holder.tvTitle.setTextColor(0xFFFF66CC.toInt())      // 标题变粉色高亮
        } else {
            // 恢复标题的默认颜色 (根据你原来的设计，通常是金色)
            holder.tvTitle.setTextColor(0xFFFFD700.toInt())

            // 原本的访问次数逻辑
            if (item.visitCount > 0) {
                holder.tvVisitCount.text = "访问 ${item.visitCount} 次"
                holder.tvVisitCount.setTextColor(0xFFFF66CC.toInt())
            } else {
                holder.tvVisitCount.text = "未访问"
                holder.tvVisitCount.setTextColor(0xFF8A7A9A.toInt())
            }
        }
        // 👆 核心逻辑结束 👆

        // 渲染标签 (去掉了背景图标签)
        holder.layoutTags.removeAllViews()
        var hasTags = false

        if (item.tags.isNotBlank()) {
            hasTags = true
            item.tags.split(",").forEach {
                if (it.isNotBlank()) {
                    addTagView(holder.layoutTags, "#${it.trim()}", 0x47FF66CC, 0xFFFF66CC.toInt())
                }
            }
        }

        if (hasTags) {
            holder.layoutTags.visibility = View.VISIBLE
        } else {
            holder.layoutTags.visibility = View.GONE
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            if (isSelectMode) {
                holder.cbSelect.isChecked = !holder.cbSelect.isChecked
                onCheckChanged?.invoke(item, holder.cbSelect.isChecked)
            } else {
                onItemClick?.invoke(item)
            }
        }

        // 长按事件
        holder.itemView.setOnLongClickListener {
            onItemLongClick?.invoke(it, item)
            true
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<WebsiteEntity>, selectMode: Boolean, selected: Set<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun addTagView(parent: LinearLayout, text: String, bgColor: Int, textColor: Int) {
        val context = parent.context
        val tv = TextView(context)
        tv.text = text
        tv.textSize = 10f
        tv.setTextColor(textColor)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = 8
        tv.layoutParams = params
        tv.setPadding(12, 4, 12, 4)

        val shape = android.graphics.drawable.GradientDrawable()
        shape.cornerRadius = 12f
        shape.setColor(bgColor)
        tv.background = shape

        parent.addView(tv)
    }
}