package com.benny.pxerstudio.activity

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.text.parseAsHtml
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.benny.pxerstudio.R
import com.benny.pxerstudio.colorpicker.ColorPicker
import com.benny.pxerstudio.databinding.ActivityDrawingBinding
import com.benny.pxerstudio.databinding.DialogActivityDrawingNewprojectBinding
import com.benny.pxerstudio.exportable.AtlasExportable
import com.benny.pxerstudio.exportable.FolderExportable
import com.benny.pxerstudio.exportable.GifExportable
import com.benny.pxerstudio.exportable.PngExportable
import com.benny.pxerstudio.shape.EraserShape
import com.benny.pxerstudio.shape.draw.BrushShape
import com.benny.pxerstudio.shape.draw.CircleShape
import com.benny.pxerstudio.shape.draw.LineShape
import com.benny.pxerstudio.shape.draw.RectShape
import com.benny.pxerstudio.shape.draw.TriangleShape
import com.benny.pxerstudio.util.prompt
import com.benny.pxerstudio.util.stripExtension
import com.benny.pxerstudio.widget.FastBitmapView
import com.benny.pxerstudio.widget.PxerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.drag.ItemTouchCallback
import com.mikepenz.fastadapter.drag.SimpleDragCallback
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.select.getSelectExtension
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DrawingActivity : AppCompatActivity(), ItemTouchCallback, PxerView.OnDropperCallBack {

    companion object {
        const val UNTITLED = "Untitled"
        val rectShapeFactory = RectShape()
        // Instance of CircleShape class. This object is used to create and manipulate circle shapes in the application.
        val circleShapeFactory = CircleShape()
        // Instance of TriangleShape class. This object is used to create and manipulate triangle shapes in the application.
        val triangleShapeFactory = TriangleShape()
        val lineShapeFactory = LineShape()
        val eraserShapeFactory = EraserShape()
        val brushShapeFactory = BrushShape()
        var currentProjectPath: String? = null

        // Used to access resources outside of activities
        lateinit var appContext: Context
        var mContext: Context
            get(): Context = appContext
            private set(value) {
                appContext = value
            }
    }

    var isEdited = false
        set(value) {
            field = value
            binding!!.drawingToolbarTextView.text =
                (
                        "PxerStudio<br><small><small>" +
                                binding!!.drawingPxerView.projectName +
                                (if (value) "*" else "") + "</small></small>"
                        ).parseAsHtml()
        }

    private lateinit var layerAdapter: FastAdapter<LayerThumbItem>
    private lateinit var layerItemAdapter: ItemAdapter<LayerThumbItem>

    private lateinit var toolsAdapter: FastAdapter<ToolItem>
    private lateinit var toolsItemAdapter: ItemAdapter<ToolItem>

    // This is a late-initialized variable for the FastAdapter. FastAdapter is a powerful and flexible RecyclerView adapter.
    private lateinit var shapesAdapter: FastAdapter<ShapesItem>

    // This is a late-initialized variable for the ItemAdapter which is a part of the FastAdapter library.
    private lateinit var shapesItemAdapter: ItemAdapter<ShapesItem>

    private lateinit var cp: ColorPicker
    private var onlyShowSelected: Boolean = false

    fun setTitle(subtitle: String?, edited: Boolean) {
        binding!!.drawingToolbarTextView.text =
            (
                    "PxerStudio<br><small><small>" +
                            if (subtitle.isNullOrEmpty()) {
                                UNTITLED
                            } else {
                                subtitle + (if (edited) "*" else "") + "</small></small>"
                            }
                    ).parseAsHtml()
        isEdited = edited
    }

    private lateinit var previousMode: PxerView.Mode

    private var binding: ActivityDrawingBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Replace Color.BLACK with the color you want to set as the background color of the DrawingActivity.
        binding?.drawingPxerView?.setBackgroundColor(Color.BLACK)
        mContext = this
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        val view = binding!!.root
        setContentView(view)

        // Set the theme based on the night mode setting
        val toolbar = findViewById<Toolbar>(R.id.drawing_toolbar)
        // Check if night mode is currently enabled
        val isDarkThemeActive = isNightModeCurrentlyEnabled()

        // If night mode is enabled, apply the dark theme
        if (isDarkThemeActive) {
            // Set the toolbar's popup theme to the dark theme
            toolbar.popupTheme = R.style.OverflowMenuThemeDark
        } else {
            // If night mode is not enabled, apply the light theme
            toolbar.popupTheme = R.style.OverflowMenuThemeLight
        }

        setTitle(UNTITLED, false)
        binding!!.drawingToolbar.title = ""
        setSupportActionBar(binding!!.drawingToolbar)
        binding!!.drawingToolbarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)

        val pxerPref = getSharedPreferences("pxerPref", Context.MODE_PRIVATE)
        binding!!.drawingPxerView.selectedColor = pxerPref.getInt("lastUsedColor", Color.YELLOW)
        binding!!.drawingPxerView.setDropperCallBack(this)

        setUpLayersView()
        // Set up the shapes menu
        setupShapesMenu()
        setupControl()

        currentProjectPath = pxerPref.getString("lastOpenedProject", null)
        print(currentProjectPath)
        if (!currentProjectPath.isNullOrEmpty()) {
            val file = File(currentProjectPath!!)
            if (file.exists()) {
                binding!!.drawingPxerView.loadProject(file)
                setTitle(file.name.stripExtension(), false)
            }
        }
        if (layerAdapter.itemCount == 0) {
            layerItemAdapter.add(LayerThumbItem())
            layerAdapter.getSelectExtension().select(0)
            layerItemAdapter.getAdapterItem(0).pressed()
        }
        System.gc()
    }

    /**
     * Overrides the default theme with a light or dark theme based on the current night mode setting.
     *
     * @author Floris
     * @return The theme to be applied. If night mode is enabled, the dark theme is returned.
     * Otherwise, the light theme is returned.
     */
    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()

        // Check if night mode is currently enabled
        if (isNightModeCurrentlyEnabled()) {
            // If night mode is enabled, apply the dark theme
            theme.applyStyle(R.style.AppThemeDark, true)
        } else {
            // If night mode is not enabled, apply the light theme
            theme.applyStyle(R.style.AppTheme, true)
        }

        // Return the theme
        return theme
    }

    override fun onColorDropped(newColor: Int) {
        binding!!.drawingFabColor.setColor(newColor)
        cp.setColor(newColor)

        binding!!.drawingFabDropper.callOnClick()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onPostCreate(savedInstanceState)
    }

    // Needed for onClick in layout
    @Suppress("UNUSED_PARAMETER")
    fun onProjectTitleClicked(view: View) {
        openProjectManager()
    }

    // Needed for onClick in layout
    @Suppress("UNUSED_PARAMETER")
    fun onToggleToolsPanel(view: View) {
        if (binding!!.drawingToolsCardView.isInvisible) {
            // If the tools panel is invisible, make it visible
            binding!!.drawingShapesCardView.isInvisible = true
            binding!!.drawingToolsCardView.isVisible = true
            binding!!.drawingToolsCardView
                .animate()
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationX(0f)
        } else {
            binding!!.drawingToolsCardView
                .animate()
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationX((+binding!!.drawingToolsCardView.width).toFloat())
                .withEndAction {
                    binding!!.drawingToolsCardView.isInvisible = true
                }
        }
    }

    /**
     * Toggles the visibility of the shapes panel.
     *
     * If the shapes panel is currently invisible, it makes it visible and hides the tools panel.
     * It also sets the alpha, scaleX, and scaleY properties of the shapes panel to 1, making it fully opaque and at its original size.
     * It then animates the shapes panel to slide in from the right.
     *
     * If the shapes panel is currently visible, it animates the shapes panel to slide out to the right and then makes it invisible.
     *
     * @author Floris
     * @param view The view that triggered this function. This parameter is not used in the function.
     */
    fun onToggleShapesPanel(view: View) {
        if (binding!!.drawingShapesCardView.isInvisible) {
            binding!!.drawingShapesCardView.isVisible = true
            binding!!.drawingToolsCardView.isInvisible = true
            binding!!.drawingShapesCardView.alpha = 1f
            binding!!.drawingShapesCardView.scaleX = 1f
            binding!!.drawingShapesCardView.scaleY = 1f
            binding!!.drawingShapesCardView
                .animate()
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationX(0f)
        } else {
            binding!!.drawingShapesCardView
                .animate()
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationX((+binding!!.drawingShapesCardView.width).toFloat())
                .withEndAction {
                    binding!!.drawingShapesCardView.isInvisible = true
                }
        }
    }

    @SuppressLint("CheckResult")
    // This function is responsible for setting up the shapes menu in the application.
    private fun setupShapesMenu() {
        // Post a runnable to the message queue of the shapes card view. This runnable adjusts the translation of the shapes card view.
        binding!!.drawingShapesCardView.post {
            binding!!.drawingShapesCardView.translationX =
                (binding!!.drawingShapesCardView.width).toFloat()
        }

        // Initialize the shapes item adapter and the shapes adapter.
        shapesItemAdapter = ItemAdapter()
        shapesAdapter = FastAdapter.with(shapesItemAdapter)

        // Set the layout manager and the adapter for the shapes recycler view.
        binding!!.drawingShapesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        binding!!.drawingShapesRecyclerView.adapter = shapesAdapter

        // Set the item animator for the shapes recycler view.
        binding!!.drawingShapesRecyclerView.itemAnimator = DefaultItemAnimator()

        // Get the select extension of the shapes adapter and configure its properties.
        val selectExtension = shapesAdapter.getSelectExtension()
        selectExtension.isSelectable = true
        selectExtension.multiSelect = false
        selectExtension.allowDeselection = true

        // Set the on click listener for the shapes adapter. This listener changes the mode and the shape tool of the Pxer view based on the clicked item.
        shapesAdapter.onClickListener = { _, _, item, _ ->
            binding!!.drawingFabShapes.setImageDrawable(ContextCompat.getDrawable(this, item.icon))
            when (item.icon) {
                R.drawable.ic_circle-> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.ShapeTool
                    binding!!.drawingPxerView.shapeTool = circleShapeFactory
                }
                R.drawable.ic_triangle -> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.ShapeTool
                    binding!!.drawingPxerView.shapeTool = triangleShapeFactory
                }
                R.drawable.ic_check_box_outline_blank -> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.ShapeTool
                    binding!!.drawingPxerView.shapeTool = rectShapeFactory
                }
                R.drawable.ic_remove -> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.ShapeTool
                    binding!!.drawingPxerView.shapeTool = lineShapeFactory
                }
            }
            false
        }

        // Add the shape items to the shapes item adapter and reverse their order.
        with(shapesItemAdapter) {
            add(ShapesItem(R.drawable.ic_circle))
            add(ShapesItem(R.drawable.ic_triangle))
            add(ShapesItem(R.drawable.ic_check_box_outline_blank))
            add(ShapesItem(R.drawable.ic_remove))
        }
        shapesItemAdapter.adapterItems.reverse()
        // Select the first item in the shapes adapter.
        shapesAdapter.getSelectExtension().select(0)
    }

    @SuppressLint("CheckResult")
    private fun setupControl() {
        binding!!.drawingToolsCardView.post {
            binding!!.drawingToolsCardView.translationX =
                (binding!!.drawingToolsCardView.width).toFloat()
        }

        toolsItemAdapter = ItemAdapter()
        toolsAdapter = FastAdapter.with(toolsItemAdapter)

        binding!!.drawingToolsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        binding!!.drawingToolsRecyclerView.adapter = toolsAdapter

        binding!!.drawingToolsRecyclerView.itemAnimator = DefaultItemAnimator()

        val selectExtension = toolsAdapter.getSelectExtension()
        selectExtension.isSelectable = true
        selectExtension.multiSelect = false
        selectExtension.allowDeselection = true

        toolsAdapter.onClickListener = { _, _, item, _ ->
            // Set the image of the tools FAB to the image of the clicked item.
            binding!!.drawingToolsFab.setImageDrawable(ContextCompat.getDrawable(this, item.icon))
            when (item.icon) {
                R.drawable.ic_format_color_fill -> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.Fill
                }
                R.drawable.ic_eraser -> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.ShapeTool
                    binding!!.drawingPxerView.shapeTool = eraserShapeFactory
                }
                R.drawable.ic_brush -> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.ShapeTool
                    binding!!.drawingPxerView.shapeTool = brushShapeFactory
                }
                R.drawable.ic_edit -> {
                    binding!!.drawingPxerView.mode = PxerView.Mode.Normal
                }
            }
            false
        }

        toolsAdapter.onLongClickListener = { _, _, item, _ ->
            // Set the image of the tools FAB to the image of the clicked item.
            binding!!.drawingToolsFab.setImageDrawable(ContextCompat.getDrawable(this, item.icon))
            when (item.icon) {
                R.drawable.ic_eraser -> {
                    MaterialDialog(this).show {
                        // Set the custom view of the dialog to the seekbar layout.
                        customView(R.layout.dialog_seekbar)
                        title(R.string.eraser_width)
                        // Get the seekbar and the text view from the custom view.
                        val seekBar = getCustomView().findViewById<SeekBar>(R.id.dialog_seekbar)
                        // Set the progress of the seekbar to the width of the eraser shape factory.
                        val valueTextView = getCustomView().findViewById<TextView>(R.id.dialog_seekbar_value)
                        // Set the text of the text view to the progress of the seekbar.
                        seekBar.progress = eraserShapeFactory.width.toInt()
                        // Set the text of the text view to the progress of the seekbar.
                        valueTextView.text = seekBar.progress.toString()
                        // Set the on seek bar change listener for the seek bar.
                        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            // Override the onProgressChanged function.
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                valueTextView.text = progress.toString()
                            }
                            // Override the onStartTrackingTouch function.
                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            // Override the onStopTrackingTouch function.
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                        })
                        // Set the positive button of the dialog to the OK button.
                        positiveButton(R.string.ok) {
                            // Set the width of the eraser shape factory to the progress of the seekbar.
                            eraserShapeFactory.width = seekBar.progress.toFloat()
                            // Set the shape tool of the Pxer view to the eraser shape factory.
                            binding!!.drawingPxerView.shapeTool = eraserShapeFactory
                        }
                    }
                }
                R.drawable.ic_brush -> {
                    MaterialDialog(this).show {
                        // Set the custom view of the dialog to the seekbar layout.
                        customView(R.layout.dialog_seekbar)
                        title(R.string.pencil_width)
                        // Get the seekbar and the text view from the custom view.
                        val seekBar = getCustomView().findViewById<SeekBar>(R.id.dialog_seekbar)
                        // Set the progress of the seekbar to the width of the brush shape factory.
                        val valueTextView = getCustomView().findViewById<TextView>(R.id.dialog_seekbar_value)
                        // Set the text of the text view to the progress of the seekbar.
                        seekBar.progress = brushShapeFactory.width.toInt()
                        // Set the text of the text view to the progress of the seekbar.
                        valueTextView.text = seekBar.progress.toString()
                        // Set the on seek bar change listener for the seek bar.
                        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            // Override the onProgressChanged function.
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                valueTextView.text = progress.toString()
                            }
                            // Override the onStartTrackingTouch function.
                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            // Override the onStopTrackingTouch function.
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                        })
                        positiveButton(R.string.ok) {
                            // Set the width of the brush shape factory to the progress of the seekbar.
                            brushShapeFactory.width = seekBar.progress.toFloat()
                            // Set the shape tool of the Pxer view to the brush shape factory.
                            binding!!.drawingPxerView.shapeTool = brushShapeFactory
                        }
                    }
                }

                R.drawable.ic_remove -> {
                    MaterialDialog(this).show {
                        // Set the custom view of the dialog to the seekbar layout.
                        customView(R.layout.dialog_seekbar)
                        title(R.string.line_width)
                        val seekBar = getCustomView().findViewById<SeekBar>(R.id.dialog_seekbar)
                        // Get the text view from the custom view.
                        val valueTextView = getCustomView().findViewById<TextView>(R.id.dialog_seekbar_value)
                        // Set the progress of the seekbar to the width of the line shape factory.
                        seekBar.progress = lineShapeFactory.width.toInt()
                        // Set the text of the text view to the progress of the seekbar.
                        valueTextView.text = seekBar.progress.toString()
                        // Set the on seek bar change listener for the seek bar.
                        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            // Override the onProgressChanged function.
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                valueTextView.text = progress.toString()
                            }
                            // Override the onStartTrackingTouch function.
                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            // Override the onStopTrackingTouch function.
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                        })
                        // Set the positive button of the dialog to the OK button.
                        positiveButton(R.string.ok) {
                            // Set the width of the line shape factory to the progress of the seekbar.
                            lineShapeFactory.width = seekBar.progress.toFloat()
                            // Set the shape tool of the Pxer view to the line shape factory.
                            binding!!.drawingPxerView.shapeTool = lineShapeFactory
                        }
                    }
                }
            }
            false
        }

        with(toolsItemAdapter) {
            add(ToolItem(R.drawable.ic_brush))
            add(ToolItem(R.drawable.ic_format_color_fill))
            add(ToolItem(R.drawable.ic_eraser))
            add(ToolItem(R.drawable.ic_edit))
        }
        toolsItemAdapter.adapterItems.reverse()
        toolsAdapter.getSelectExtension().select(0)

        cp = ColorPicker(
            this,
            binding!!.drawingPxerView.selectedColor,
        ) { newColor ->
            binding!!.drawingPxerView.selectedColor = newColor
            binding!!.drawingFabColor.setColor(newColor)
        }
        binding!!.drawingFabColor.setOnClickListener(cp::show)
        binding!!.drawingFabUndo.setOnClickListener { binding!!.drawingPxerView.undo() }
        binding!!.drawingFabRedo.setOnClickListener { binding!!.drawingPxerView.redo() }
        binding!!.drawingFabDropper.setOnClickListener {
            if (binding!!.drawingPxerView.mode == PxerView.Mode.Dropper) {
                binding!!.drawingFabUndo.show(true)
                binding!!.drawingFabRedo.show(true)

                binding!!.drawingToolsFab.show(true)

                binding!!.drawingPxerView.mode = previousMode

                binding!!.drawingFabDropper.setImageResource(R.drawable.ic_colorize)
            } else {
                binding!!.drawingFabUndo.hide(true)
                binding!!.drawingFabRedo.hide(true)

                binding!!.drawingToolsFab.hide(true)
                if (binding!!.drawingToolsCardView.isVisible) {
                    binding!!.drawingToolsFab.callOnClick()
                }

                previousMode = binding!!.drawingPxerView.mode
                binding!!.drawingPxerView.mode = PxerView.Mode.Dropper

                binding!!.drawingFabDropper.setImageResource(R.drawable.ic_close)
            }
        }
    }

    private fun setUpLayersView() {
        binding!!.drawingLayerAddCardView.setOnClickListener {
            binding!!.drawingPxerView.addLayer()
            layerItemAdapter.add(max(binding!!.drawingPxerView.currentLayer, 0), LayerThumbItem())
            layerAdapter.getSelectExtension().deselect()
            layerAdapter.getSelectExtension().select(binding!!.drawingPxerView.currentLayer)
            layerItemAdapter.getAdapterItem(binding!!.drawingPxerView.currentLayer).pressed()
            binding!!.drawingLayerRecyclerView.invalidate()
        }

        layerItemAdapter = ItemAdapter()
        layerAdapter = FastAdapter.with(layerItemAdapter)

        binding!!.drawingLayerRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding!!.drawingLayerRecyclerView.adapter = layerAdapter

        binding!!.drawingLayerRecyclerView.itemAnimator = DefaultItemAnimator()
        (binding!!.drawingLayerRecyclerView.itemAnimator as DefaultItemAnimator).changeDuration = 0
        (binding!!.drawingLayerRecyclerView.itemAnimator as DefaultItemAnimator).addDuration = 0
        (binding!!.drawingLayerRecyclerView.itemAnimator as DefaultItemAnimator).removeDuration = 0

        val touchCallback = SimpleDragCallback(this)
        val touchHelper = ItemTouchHelper(touchCallback)
        touchHelper.attachToRecyclerView(binding!!.drawingLayerRecyclerView)

        val selectExtension = layerAdapter.getSelectExtension()
        selectExtension.isSelectable = true
        selectExtension.multiSelect = false
        selectExtension.allowDeselection = false

        layerAdapter.onLongClickListener = { _, _, item, position ->
            selectExtension.deselect()
            selectExtension.select(position)

            item.pressed()
            false
        }
        layerAdapter.onClickListener = { v, _, item, position ->
            for (_item in layerItemAdapter.adapterItems)
                if (!_item.isSelected) {
                    _item.pressedTime = 0
                }

            if (onlyShowSelected) {
                val layer =
                    binding!!.drawingPxerView.pxerLayers[binding!!.drawingPxerView.currentLayer]
                layer!!.visible = false
                binding!!.drawingPxerView.invalidate()

                layerAdapter.notifyAdapterItemChanged(binding!!.drawingPxerView.currentLayer)
            }
            binding!!.drawingPxerView.currentLayer = position
            if (onlyShowSelected) {
                val layer =
                    binding!!.drawingPxerView.pxerLayers[binding!!.drawingPxerView.currentLayer]
                layer!!.visible = true
                binding!!.drawingPxerView.invalidate()

                layerAdapter.notifyAdapterItemChanged(binding!!.drawingPxerView.currentLayer)
            }
            item.pressed()
            if (item.isPressSecondTime) {
                val popupMenu = PopupMenu(this@DrawingActivity, v!!)
                popupMenu.inflate(R.menu.menu_popup_layer)
                popupMenu.setOnMenuItemClickListener { clickedItem ->
                    this@DrawingActivity.onOptionsItemSelected(clickedItem)
                    false
                }
                popupMenu.show()
            }
            true
        }
    }

    override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
        if (!isEdited) {
            isEdited = true
        }

        binding!!.drawingPxerView.moveLayer(oldPosition, newPosition)

        if (oldPosition < newPosition) {
            for (i in oldPosition + 1..newPosition) {
                Collections.swap(layerItemAdapter.adapterItems, i, i - 1)
                layerAdapter.notifyAdapterItemMoved(i, i - 1)
            }
        } else {
            for (i in oldPosition - 1 downTo newPosition) {
                Collections.swap(layerItemAdapter.adapterItems, i, i + 1)
                layerAdapter.notifyAdapterItemMoved(i, i + 1)
            }
        }

        return true
    }

    override fun itemTouchDropped(oldPosition: Int, newPosition: Int) {
        binding!!.drawingPxerView.currentLayer = newPosition
    }

    fun onLayerUpdate() {
        layerItemAdapter.clear()
        for (i in 0 until binding!!.drawingPxerView.pxerLayers.size) {
            layerItemAdapter.add(LayerThumbItem())
        }
        layerAdapter.getSelectExtension().select(0)
        layerItemAdapter.getAdapterItem(0).pressed()
    }

    fun onLayerRefresh() {
        if (binding != null) {
            binding!!.drawingLayerRecyclerView.invalidate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Set the theme based on the night mode setting
        if (isNightModeCurrentlyEnabled()) {
            // If night mode is enabled, apply the dark theme
            setTheme(R.style.AppThemeDark)
        } else {
            // If night mode is not enabled, apply the light theme
            setTheme(R.style.AppTheme)
        }
        menuInflater.inflate(R.menu.menu_activity_drawing, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private val myFilter: FileFilter =
        { it.isDirectory || it.name.endsWith(PxerView.PXER_EXTENSION_NAME, true) }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_drawing_layers_onlyShowSelected -> {
                onlyShowSelected = true
                binding!!.drawingPxerView.visibilityAllLayer(false)

                val layer2 =
                    binding!!.drawingPxerView.pxerLayers[binding!!.drawingPxerView.currentLayer]
                layer2!!.visible = true
                binding!!.drawingPxerView.invalidate()

                layerAdapter.notifyAdapterDataSetChanged()
            }
            R.id.menu_drawing_export_png -> PngExportable()
                .runExport(this, binding!!.drawingPxerView)
            R.id.menu_drawing_export_gif -> GifExportable()
                .runExport(this, binding!!.drawingPxerView)
            R.id.menu_drawing_export_folder -> FolderExportable()
                .runExport(this, binding!!.drawingPxerView)
            R.id.menu_drawing_export_atlas -> AtlasExportable()
                .runExport(this, binding!!.drawingPxerView)
            R.id.menu_drawing_project_save -> binding!!.drawingPxerView.save(true)
            R.id.menu_drawing_project_manager -> openProjectManager()
            R.id.menu_drawing_project_open ->
                MaterialDialog(this).show {
                    fileChooser(
                        filter = myFilter,
                        initialDirectory = File(
                            context.getExternalFilesDir("/")!!,
                            "/PxerStudio/Project",
                        ),
                        context = context,
                    ) { _, file ->
                        binding!!.drawingPxerView.loadProject(file)
                        setTitle(file.name.stripExtension(), false)
                        currentProjectPath = file.path
                    }
                }
            R.id.menu_drawing_project_new -> createNewProject()
            R.id.menu_drawing_resetViewPort -> binding!!.drawingPxerView.resetViewPort()
            R.id.menu_drawing_layers_hideAll -> run {
                if (onlyShowSelected) return@run
                binding!!.drawingPxerView.visibilityAllLayer(false)
                layerAdapter.notifyAdapterDataSetChanged()
            }
            R.id.menu_drawing_layers_showAll -> {
                onlyShowSelected = false
                binding!!.drawingPxerView.visibilityAllLayer(true)
                layerAdapter.notifyAdapterDataSetChanged()
            }
            R.id.menu_drawing_gridOnOff -> {
                if (binding!!.drawingPxerView.showGrid) {
                    item.setIcon(R.drawable.ic_grid_on)
                } else {
                    item.setIcon(R.drawable.ic_grid_off)
                }
                binding!!.drawingPxerView.showGrid = !binding!!.drawingPxerView.showGrid
            }
            R.id.menu_drawing_layers -> {
                binding!!.drawingLayerCardView.pivotX =
                    (binding!!.drawingLayerCardView.width / 2).toFloat()
                binding!!.drawingLayerCardView.pivotY = 0f
                if (binding!!.drawingLayerCardView.isVisible) {
                    binding!!.drawingLayerCardView
                        .animate()
                        .setDuration(100)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .alpha(0f)
                        .scaleX(0.85f)
                        .scaleY(0.85f)
                        .withEndAction {
                            binding!!.drawingLayerCardView.isInvisible = true
                        }
                } else {
                    binding!!.drawingLayerCardView.isVisible = true
                    binding!!.drawingLayerCardView
                        .animate()
                        .setDuration(100)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                }
//                layer_view.alpha = 1f
//                layer_view.scaleX = 1f
//                layer_view.scaleY = 1f

//                if (layer_view.visibility == View.VISIBLE)
//                    layer_view!!.visibility = View.INVISIBLE
//                else
//                    layer_view.visibility = View.VISIBLE
            }
            R.id.menu_popup_layer_remove -> run {
                if (binding!!.drawingPxerView.pxerLayers.size <= 1) return@run
                prompt()
                    .title(R.string.remove_layer)
                    .message(R.string.remove_layer_warning)
                    .positiveButton(R.string.remove)
                    .positiveButton {
                        if (!isEdited) {
                            isEdited = true
                        }

                        layerItemAdapter.remove(binding!!.drawingPxerView.currentLayer)
                        binding!!.drawingPxerView.removeCurrentLayer()

                        layerAdapter.getSelectExtension().deselect()
                        layerAdapter.getSelectExtension()
                            .select(binding!!.drawingPxerView.currentLayer)
                        layerItemAdapter.getAdapterItem(binding!!.drawingPxerView.currentLayer)
                            .pressed()
                        layerAdapter.notifyAdapterDataSetChanged()
                    }
                    .show()
            }
            R.id.menu_popup_layer_duplicate -> {
                binding!!.drawingPxerView.copyAndPasteCurrentLayer()
                layerItemAdapter.add(
                    max(binding!!.drawingPxerView.currentLayer, 0),
                    LayerThumbItem(),
                )
                layerAdapter.getSelectExtension().deselect()
                layerAdapter.getSelectExtension().select(binding!!.drawingPxerView.currentLayer)
                layerItemAdapter.getAdapterItem(binding!!.drawingPxerView.currentLayer).pressed()
                binding!!.drawingLayerRecyclerView.invalidate()
            }
            R.id.menu_drawing_layers_mergeAll -> run {
                if (binding!!.drawingPxerView.pxerLayers.size <= 1) return@run
                prompt()
                    .title(R.string.merge_all_layers)
                    .message(R.string.merge_all_layers_warning)
                    .positiveButton(R.string.merge)
                    .positiveButton {
                        if (!isEdited) {
                            isEdited = true
                        }

                        binding!!.drawingPxerView.mergeAllLayers()
                        layerItemAdapter.clear()
                        layerItemAdapter.add(LayerThumbItem())
                        layerAdapter.getSelectExtension().deselect()
                        layerAdapter.getSelectExtension().select(0)
                        layerItemAdapter.getAdapterItem(0).pressed()
                    }
                    .show()
            }
            R.id.menu_drawing_about -> startActivity(
                Intent(
                    this@DrawingActivity,
                    AboutActivity::class.java,
                ),
            )
            R.id.menu_popup_layer_toggleVisibility -> run {
                if (onlyShowSelected) return@run
                val layer =
                    binding!!.drawingPxerView.pxerLayers[binding!!.drawingPxerView.currentLayer]
                layer!!.visible = !layer.visible
                binding!!.drawingPxerView.invalidate()
                layerAdapter.notifyAdapterItemChanged(binding!!.drawingPxerView.currentLayer)
            }
            R.id.menu_popup_layer_clear -> prompt()
                .title(R.string.clear_current_layer)
                .message(R.string.clear_current_layer_warning)
                .positiveButton(R.string.clear)
                .positiveButton { binding!!.drawingPxerView.clearCurrentLayer() }
                .show()
            R.id.menu_popup_layer_mergeDown -> run {
                if (binding!!.drawingPxerView.currentLayer == binding!!.drawingPxerView.pxerLayers.size - 1) return@run
                prompt()
                    .title(R.string.merge_down_layer)
                    .message(R.string.merge_down_layer_warning)
                    .positiveButton(R.string.merge)
                    .positiveButton {
                        binding!!.drawingPxerView.mergeDownLayer()
                        layerItemAdapter.remove(binding!!.drawingPxerView.currentLayer + 1)
                        layerAdapter.getSelectExtension()
                            .select(binding!!.drawingPxerView.currentLayer)
                        layerItemAdapter.getAdapterItem(binding!!.drawingPxerView.currentLayer)
                            .pressed()
                    }
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openProjectManager() {
        binding!!.drawingPxerView.save(false)
        startActivityForResult(Intent(this, ProjectManagerActivity::class.java), 659)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == 659 && data != null) {
            val path = data.getStringExtra("selectedProjectPath")
            if (path != null && path.isNotEmpty()) {
                currentProjectPath = path
                val file = File(path)
                if (file.exists()) {
                    binding!!.drawingPxerView.loadProject(file)
                    setTitle(file.name.stripExtension(), false)
                }
            } else if (data.getBooleanExtra("fileNameChanged", false)) {
                currentProjectPath = ""
                binding!!.drawingPxerView.projectName = ""
                recreate()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createNewProject() {
        val newprojectBinding = DialogActivityDrawingNewprojectBinding.inflate(layoutInflater)
        val layoutRoot = newprojectBinding.root

        val projectNameEdit = newprojectBinding.dialogDrawingNpNameEdit
        val widthSeekBar = newprojectBinding.dialogDrawingNpWidthSeekBar
        val widthText = newprojectBinding.dialogDrawingNpWidth
        val heightSeekBar = newprojectBinding.dialogDrawingNpHeightSeekBar
        val heightText = newprojectBinding.dialogDrawingNpHeight

        widthSeekBar.max = 127
        widthSeekBar.progress = 39
        widthText.text = resources.getText(R.string.width).toString() + 40
        widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                widthText.text = resources.getText(R.string.width).toString() + (i + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        heightSeekBar.max = 127
        heightSeekBar.progress = 39
        heightText.text = resources.getText(R.string.width).toString() + 40
        heightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                heightText.text = resources.getText(R.string.width).toString() + (i + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        MaterialDialog(this)
            .customView(view = layoutRoot)
            .title(R.string.new_project)
            .positiveButton(R.string.create)
            .negativeButton(android.R.string.cancel)
            .positiveButton {
                if (projectNameEdit.text.toString().isNotEmpty()) {
                    setTitle(projectNameEdit.text.toString(), true)
                    binding!!.drawingPxerView.createBlankProject(
                        projectNameEdit.text.toString(),
                        widthSeekBar.progress + 1,
                        heightSeekBar.progress + 1,
                    )
                }
            }
            .show()
        binding!!.drawingPxerView.save(false)
    }

    override fun onStop() {
        saveState()
        super.onStop()
    }

    private fun saveState() {
        val pxerPref = getSharedPreferences("pxerPref", Context.MODE_PRIVATE)
        pxerPref.edit {
            putString("lastOpenedProject", currentProjectPath)
            putInt("lastUsedColor", binding!!.drawingPxerView.selectedColor)
        }
        if (
            !binding!!.drawingPxerView.projectName.isNullOrEmpty() ||
            binding!!.drawingPxerView.projectName != UNTITLED
        ) {
            binding!!.drawingPxerView.save(false)
        } else {
            binding!!.drawingPxerView.save(true)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cp.onConfigChanges()
    }

    private inner class LayerThumbItem : AbstractItem<LayerThumbItem.ViewHolder>() {
        var pressedTime = 0

        val isPressSecondTime: Boolean
            get() = pressedTime == 2

        fun pressed() {
            pressedTime++
            pressedTime = min(2, pressedTime)
        }

        override val type: Int
            get() = R.id.item_layer_thumb

        override val layoutRes: Int
            get() = R.layout.item_layer_thumb

//        override fun withSetSelected(selected: Boolean): LayerThumbItem {
//            if (!selected)
//                pressedTime = 0
//            return super.withSetSelected(selected)
//        }

        override fun getViewHolder(v: View): ViewHolder {
            return ViewHolder(v)
        }

        inner class ViewHolder(view: View) : FastAdapter.ViewHolder<LayerThumbItem>(view) {
            var iv: FastBitmapView = view as FastBitmapView

            override fun bindView(item: LayerThumbItem, payloads: List<Any>) {
                iv.isSelected = item.isSelected

                val layer = binding!!.drawingPxerView.pxerLayers[layoutPosition]
                iv.setVisible(layer!!.visible)
                iv.bitmap = layer.bitmap
            }

            override fun unbindView(item: LayerThumbItem) {}
        }
    }

    private inner class ToolItem(var icon: Int) : AbstractItem<ToolItem.ViewHolder>() {

        override val type: Int
            get() = R.id.item_tool

        override val layoutRes: Int
            get() = R.layout.item_tool

//        override fun isSelectable(): Boolean {
//            return true
//        }

        override fun getViewHolder(v: View): ViewHolder {
            return ViewHolder(v)
        }

        inner class ViewHolder(view: View) : FastAdapter.ViewHolder<ToolItem>(view) {
            var iv: ImageView = view as ImageView

            override fun bindView(item: ToolItem, payloads: List<Any>) {
                if (isSelected) {
                    iv.alpha = 1f
                } else {
                    iv.alpha = 0.3f
                }

                iv.setImageResource(item.icon)
            }

            override fun unbindView(item: ToolItem) {}
        }
    }
    /**
     * This inner class represents a shape item for the FastAdapter.
     * Each shape item has an icon that is used to represent it in the UI.
     *
     * @property icon The resource ID of the icon for this shape item.
     */
    private inner class ShapesItem(var icon: Int) : AbstractItem<ShapesItem.ViewHolder>() {

        // The type of this item. This is used by the FastAdapter to differentiate between different types of items.
        override val type: Int
            get() = R.id.item_shape

        // The layout resource for this item. This is used by the FastAdapter to inflate the view for this item.
        override val layoutRes: Int
            get() = R.layout.item_shape

        // This function is used by the FastAdapter to get a ViewHolder for this item.
        override fun getViewHolder(v: View): ViewHolder {
            return ViewHolder(v)
        }

        /**
         * This inner class represents the ViewHolder for a ShapesItem.
         * It contains the logic for binding a ShapesItem to the view.
         *
         * @property iv The ImageView that displays the icon for the shape item.
         */
        inner class ViewHolder(view: View) : FastAdapter.ViewHolder<ShapesItem>(view) {
            var iv: ImageView = view as ImageView

            // This function is used by the FastAdapter to bind a ShapesItem to the ViewHolder.
            override fun bindView(item: ShapesItem, payloads: List<Any>) {
                if (isSelected) {
                    iv.alpha = 1f
                } else {
                    iv.alpha = 0.3f
                }

                iv.setImageResource(item.icon)
            }

            // This function is used by the FastAdapter to unbind a ShapesItem from the ViewHolder.
            override fun unbindView(item: ShapesItem) {}
        }
    }

    /**
     * This function checks if the night mode is currently enabled in the application.
     *
     * @return True if the night mode is enabled, false otherwise.
     */
    fun isNightModeCurrentlyEnabled(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
    }
}
