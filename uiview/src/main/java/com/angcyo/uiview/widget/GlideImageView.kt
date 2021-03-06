package com.angcyo.uiview.widget

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.angcyo.github.utilcode.utils.ImageUtils
import com.angcyo.library.okhttp.Ok
import com.angcyo.library.utils.L
import com.angcyo.uiview.R
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.GenericRequest
import java.io.File

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：
 * 创建人员：Robi
 * 创建时间：2017/07/04 11:11
 * 修改人员：Robi
 * 修改时间：2017/07/04 11:11
 * 修改备注：
 * Version: 1.0.0
 */
open class GlideImageView(context: Context, attributeSet: AttributeSet? = null) : RImageView(context, attributeSet) {
    /**是否要检查Url的图片类型为Gif, 可以用来显示Gif指示图*/
    var checkGif = false

    /**强制url使用asGif的方式加载, 需要 checkGif=true*/
    var showGifImage = false

    /**占位资源*/
    var placeholderRes: Int = R.drawable.base_image_placeholder_shape

    var override = true

    /**需要加载的图片地址, 优先判断是否是本地File*/
    var url: String? = null
        set(value) {
            field = value
            startLoadUrl()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        startLoadUrl()
    }

    //启动加载流程
    private fun startLoadUrl() {
        setShowGifTip(false)

        url?.let {
            if (measuredHeight == 0 || measuredWidth == 0) {

            } else {
                val file = File(it)
                if (file.exists()) {
                    loadWithFile(file)
                } else {
                    loadWidthUrl(it)
                }
            }
        }
    }

    fun setTagUrl(url: String) {
        setTag(R.id.tag_url, url)
    }

    /**取消请求*/
    fun cancelRequest() {
        val tag = this.tag
        if (tag is GenericRequest<*, *, *, *>) {
            tag.clear()
            L.d("cancelRequest() ->" + this.javaClass.simpleName + "  GenericRequest Clear")
        }
    }

    //加载方法
    private fun loadWithFile(file: File) {
        if (checkGif) {
            if ("GIF".equals(ImageUtils.getImageType(file), ignoreCase = true)) {
                if (showGifImage) {
                    showGifFile(file)
                } else {
                    setShowGifTip(true)
                    showJpegFile(file)
                }
            } else {
                showJpegFile(file)
            }
        } else {
            showFile(file)
        }
    }


    private fun showGifFile(file: File) {
        val builder = Glide.with(context)
                .load(file)
                .asGif()
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .crossFade()
                .priority(Priority.HIGH)
        if (override) {
            if (measuredWidth == 0 || measuredHeight == 0) {
                post {
                    showGifFile(file)
                }
            } else {
                builder.override(measuredWidth, measuredHeight).into(this)
            }
        } else {
            builder.into(this)
        }
    }

    private fun showJpegFile(file: File) {
        val builder = Glide.with(context)
                .load(file)
                .asBitmap()
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .priority(Priority.NORMAL)
        if (override) {
            if (measuredWidth == 0 || measuredHeight == 0) {
                post {
                    showJpegFile(file)
                }
            } else {
                builder.override(measuredWidth, measuredHeight).into(this)
            }
        } else {
            builder.into(this)
        }
    }

    private fun showFile(file: File) {
        val builder = Glide.with(context)
                .load(file)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .priority(Priority.NORMAL)

        if (override) {
            if (measuredWidth == 0 || measuredHeight == 0) {
                post {
                    showFile(file)
                }
            } else {
                builder.override(measuredWidth, measuredHeight).into(this)
            }
        } else {
            builder.into(this)
        }
    }

    private fun loadWidthUrl(url: String) {
        setTag(R.id.tag_url, url)
        if (checkGif) {
            Ok.instance()
                    .type(url, object : Ok.OnImageTypeListener {
                        override fun onLoadStart() {
                            this@GlideImageView.setImageResource(placeholderRes)
                        }

                        override fun onImageType(imageUrl: String, imageType: Ok.ImageType) {
                            val tagUrl = getTag(R.id.tag_url)?.toString()

                            if (tagUrl.isNullOrEmpty()) {
                                return
                            }

                            if (!imageUrl.contains(tagUrl!!)) {
                                return
                            }

                            if (context is Activity) {
                                if ((context as Activity).isDestroyed) {
                                    return
                                }
                            }

                            if (imageType == Ok.ImageType.GIF) {
                                if (showGifImage) {
                                    loadGifUrl(imageUrl)
                                } else {
                                    setShowGifTip(true)
                                    loadJpegUrl(imageUrl)
                                }
                            } else {
                                loadJpegUrl(imageUrl)
                            }
                        }
                    })
        } else {
            loadUrl(url)
        }
    }

    private fun loadUrl(url: String) {
        val builder = Glide.with(context)
                .load(url)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.NORMAL)
                .crossFade()
        if (override) {
            if (measuredWidth == 0 || measuredHeight == 0) {
                post {
                    loadUrl(url)
                }
            } else {
                builder.override(measuredWidth, measuredHeight).into(this)
            }
        } else {
            builder.into(this)
        }
    }

    private fun loadJpegUrl(url: String) {
        val builder = Glide.with(context)
                .load(url)
                .asBitmap()
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.NORMAL)
                .crossFade()
        if (override) {
            if (measuredWidth == 0 || measuredHeight == 0) {
                post {
                    loadJpegUrl(url)
                }
            } else {
                builder.override(measuredWidth, measuredHeight).into(this)
            }
        } else {
            builder.into(this)
        }
    }

    private fun loadGifUrl(url: String) {
        val builder = Glide.with(context)
                .load(url)
                .asGif()
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .priority(Priority.HIGH)
                .crossFade()
        if (override) {
            if (measuredWidth == 0 || measuredHeight == 0) {
                post {
                    loadGifUrl(url)
                }
            } else {
                builder.override(measuredWidth, measuredHeight).into(this)
            }
        } else {
            builder.into(this)
        }
    }

}
