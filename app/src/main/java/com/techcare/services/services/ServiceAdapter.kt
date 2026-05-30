package com.techcare.services.services

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.techcare.services.databinding.ItemServiceBinding

class ServiceAdapter(
    private var list: List<ServiceItem>,
    private val onBook: (ServiceItem) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.VH>() {

    inner class VH(val b: ItemServiceBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = list[position]
        with(holder.b) {
            tvIcon.text = s.icon
            tvName.text = s.name
            tvDescription.text = s.description
            tvPrice.text = "From LKR ${"%,d".format(s.price)}"
            tvRating.text = "⭐ ${s.rating}"
            tvCategory.text = s.category
            btnBook.setOnClickListener { onBook(s) }
            root.setOnClickListener { onBook(s) }
        }
    }

    fun updateList(newList: List<ServiceItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = 
                list[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = 
                list[oldPos] == newList[newPos]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }
}