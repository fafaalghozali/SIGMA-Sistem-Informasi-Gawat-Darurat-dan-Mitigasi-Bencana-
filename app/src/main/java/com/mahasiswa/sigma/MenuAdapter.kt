package com.mahasiswa.sigma

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.mahasiswa.sigma.databinding.ItemMenuBinding

class MenuAdapter(
    private val listMenu: List<MenuModel>,
    private val onItemClick: (MenuModel) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menu = listMenu[position]
        with(holder.binding) {
            tvMenuTitle.text = menu.title
            tvMenuDesc.text = menu.description
            imgIcon.setImageResource(menu.iconRes)
            
            root.setOnClickListener {
                // Scale animation on click
                it.startAnimation(AnimationUtils.loadAnimation(it.context, android.R.anim.fade_in))
                onItemClick(menu)
            }
        }
    }

    override fun getItemCount(): Int = listMenu.size
}
