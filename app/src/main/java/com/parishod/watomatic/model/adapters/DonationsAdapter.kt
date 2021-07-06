package com.parishod.watomatic.model.adapters

import com.parishod.watomatic.model.data.DonationProgressItem
import com.transferwise.sequencelayout.SequenceAdapter
import com.transferwise.sequencelayout.SequenceStep

class DonationsAdapter(private val items: List<DonationProgressItem>) : SequenceAdapter<DonationProgressItem>() {

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): DonationProgressItem {
        return items[position]
    }

    override fun bindView(sequenceStep: SequenceStep, item: DonationProgressItem) {
        with(sequenceStep) {
            setActive(item.isActive)
            setAnchor(item.formattedDate)
//            setAnchorTextAppearance(...)
            setTitle(item.title)
//            setTitleTextAppearance()
            setSubtitle(item.subTitle)
//            setSubtitleTextAppearance(...)
        }
    }
}