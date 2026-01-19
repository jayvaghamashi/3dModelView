package org.the3deer.android_3d_model_engine.services.collada;



import android.graphics.Bitmap;

import android.graphics.BitmapFactory;

import android.graphics.Canvas;

import android.graphics.Color;

import android.graphics.Rect;

import android.opengl.GLES20;

import android.util.Log;



import androidx.annotation.NonNull;



import org.the3deer.android_3d_model_engine.animation.Animation;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;

import org.the3deer.android_3d_model_engine.model.Constants;

import org.the3deer.android_3d_model_engine.model.Element;

import org.the3deer.android_3d_model_engine.model.Object3DData;

import org.the3deer.android_3d_model_engine.model.Texture;

import org.the3deer.android_3d_model_engine.services.LoadListener;

import org.the3deer.android_3d_model_engine.services.collada.entities.JointData;

import org.the3deer.android_3d_model_engine.services.collada.entities.MeshData;

import org.the3deer.android_3d_model_engine.services.collada.entities.SkeletonData;

import org.the3deer.android_3d_model_engine.services.collada.entities.SkinningData;

import org.the3deer.android_3d_model_engine.services.collada.loader.AnimationLoader;

import org.the3deer.android_3d_model_engine.services.collada.loader.GeometryLoader;

import org.the3deer.android_3d_model_engine.services.collada.loader.MaterialLoader;

import org.the3deer.android_3d_model_engine.services.collada.loader.SkeletonLoader;

import org.the3deer.android_3d_model_engine.services.collada.loader.SkinLoader;

import org.the3deer.util.android.ContentUtils;

import org.the3deer.util.io.IOUtils;

import org.the3deer.util.xml.XmlNode;

import org.the3deer.util.xml.XmlParser;



import java.io.ByteArrayOutputStream;

import java.io.InputStream;

import java.net.URI;

import java.util.ArrayList;

import java.util.Arrays;

import java.util.Collections;

import java.util.List;

import java.util.Map;



public final class ColladaLoader {



    // Enums for Mapping Logic

    private enum Part { BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }

    private enum Side { FRONT, BACK, LEFT, RIGHT, UP, DOWN }



    public static List<String> getImages(InputStream is) {

        try {

            final XmlNode xml = XmlParser.parse(is);

            return new MaterialLoader(xml.getChild("library_materials"),

                    xml.getChild("library_effects"), xml.getChild("library_images")).getImages();

        } catch (Exception ex) {

            Log.e("ColladaLoaderTask", "Error loading materials", ex);

            return null;

        }

    }



    @NonNull

    public List<Object3DData> load(URI uri, LoadListener callback) {

        final List<Object3DData> ret = new ArrayList<>();

        final List<MeshData> allMeshes = new ArrayList<>();



        try (InputStream is = ContentUtils.getInputStream(uri)) {



            Log.i("ColladaLoaderTask", "Parsing file... " + uri.toString());

            callback.onProgress("Loading file...");

            final XmlNode xml = XmlParser.parse(is);



            String authoring_tool = null;

            try {

                XmlNode child = xml.getChild("asset").getChild("contributor").getChild("authoring_tool");

                authoring_tool = child.getData();

            } catch (Exception e) { }



            // Visual Scene

            Log.i("ColladaLoaderTask", "Loading visual nodes...");

            Map<String, SkeletonData> skeletons = null;

            try {

                SkeletonLoader jointsLoader = new SkeletonLoader(xml);

                skeletons = jointsLoader.loadJoints();

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error loading visual scene", ex);

            }



            // Geometries

            Log.i("ColladaLoaderTask", "Loading geometries...");

            List<MeshData> meshDatas = null;

            try {

                GeometryLoader g = new GeometryLoader(xml.getChild("library_geometries"));

                List<XmlNode> geometries = xml.getChild("library_geometries").getChildren("geometry");

                meshDatas = new ArrayList<>();

                for (int i = 0; i < geometries.size(); i++) {

                    XmlNode geometry = geometries.get(i);

                    MeshData meshData = g.loadGeometry(geometry);

                    if (meshData == null) continue;

                    meshDatas.add(meshData);

                    allMeshes.add(meshData);



                    AnimatedModel data3D = new AnimatedModel(meshData.getVertexBuffer(), null);

                    data3D.setAuthoringTool(authoring_tool);

                    data3D.setMeshData(meshData);

                    data3D.setId(meshData.getId());

                    data3D.setVertexBuffer(meshData.getVertexBuffer());

                    data3D.setNormalsBuffer(meshData.getNormalsBuffer());

                    data3D.setColorsBuffer(meshData.getColorsBuffer());

                    data3D.setElements(meshData.getElements());

                    data3D.setDrawMode(GLES20.GL_TRIANGLES);

                    data3D.setDrawUsingArrays(false);



                    if (skeletons != null) {

                        JointData jointData = null;

                        SkeletonData skeletonData = skeletons.get(meshData.getId());

                        if (skeletonData == null) skeletonData = skeletons.get("default");

                        if (skeletonData != null) jointData = skeletonData.find(meshData.getId());

                        if (jointData != null) {

                            data3D.setName(jointData.getName());

                            data3D.setBindTransform(jointData.getBindTransform());

                        }

                    }

                    callback.onLoad(data3D);

                    ret.add(data3D);

                }

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error loading geometries", ex);

                return Collections.emptyList();

            }



            // Materials

            Log.i("ColladaLoaderTask", "Loading materials...");

            try {

                final MaterialLoader materialLoader = new MaterialLoader(xml.getChild("library_materials"),

                        xml.getChild("library_effects"), xml.getChild("library_images"));

                for (int i = 0; i < meshDatas.size(); i++) {

                    final MeshData meshData = meshDatas.get(i);

                    final Object3DData data3D = ret.get(i);

                    materialLoader.loadMaterial(meshData);

                    data3D.setTextureBuffer(meshData.getTextureBuffer());

                }

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error loading materials", ex);

            }



            // Visual Scene binding

            try {

                final MaterialLoader materialLoader = new MaterialLoader(xml.getChild("library_materials"),

                        xml.getChild("library_effects"), xml.getChild("library_images"));

                for (int i = 0; i < meshDatas.size(); i++) {

                    final MeshData meshData = meshDatas.get(i);

                    SkeletonData skeletonData = skeletons.get(meshData.getId());

                    if (skeletonData == null) skeletonData = skeletons.get("default");

                    materialLoader.loadMaterialFromVisualScene(meshData, skeletonData);

                }



                for (int i = 0; i < meshDatas.size(); i++) {

                    final MeshData meshData = meshDatas.get(i);

                    final AnimatedModel data3D = (AnimatedModel) ret.get(i);

                    SkeletonData skeletonData = skeletons.get(meshData.getId());

                    if (skeletonData == null) skeletonData = skeletons.get("default");

                    if (skeletonData == null) continue;



                    List<JointData> allJointData = skeletonData.getHeadJoint().findAll(meshData.getId());

                    if (allJointData.isEmpty()) continue;



                    if (allJointData.size() == 1) {

                        data3D.setBindTransform(allJointData.get(0).getBindTransform());

                        continue;

                    }



                    boolean isOriginalMeshConfigured = false;

                    for (JointData jd : allJointData) {

                        if (!isOriginalMeshConfigured) {

                            data3D.setBindTransform(jd.getBindTransform());

                            isOriginalMeshConfigured = true;

                            continue;

                        }

                        final AnimatedModel instance_geometry = data3D.clone();

                        instance_geometry.setId(data3D.getId() + "_instance_" + jd.getName());

                        instance_geometry.setBindTransform(jd.getBindTransform());

                        callback.onLoad(instance_geometry);

                        ret.add(instance_geometry);

                        allMeshes.add(meshData.clone());

                    }

                }

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error loading visual scene", ex);

            }



            // -------------------------------------------------------------------------------------

            // ROBLOX TEXTURE MAPPING

            // -------------------------------------------------------------------------------------

            Log.i("ColladaLoaderTask", "--------------------------------------------------");

            Log.i("ColladaLoaderTask", "Loading and Mapping Skin Textures...");

            Log.i("ColladaLoaderTask", "--------------------------------------------------");

            callback.onProgress("Loading textures...");



            try {

                for (int i = 0; i < meshDatas.size(); i++) {

                    final MeshData meshData = meshDatas.get(i);

                    for (int e = 0; e < meshData.getElements().size(); e++) {

                        final Element element = meshData.getElements().get(e);



                        if (element.getMaterial() != null) {



                            if (element.getMaterial().getColorTexture() == null) {

                                element.getMaterial().setColorTexture(new Texture());

                            }



                            String texturePath;

                            boolean isFallback = false;



                            if (element.getMaterial().getColorTexture().getFile() != null) {

                                texturePath = element.getMaterial().getColorTexture().getFile();

                            } else {

                                String modelUriStr = uri.toString();

                                int lastSlash = modelUriStr.lastIndexOf('/');

                                if (lastSlash != -1) {

                                    texturePath = modelUriStr.substring(0, lastSlash + 1) + "skin.png";

                                } else {

                                    texturePath = "skin.png";

                                }

                                isFallback = true;

                                Log.i("ColladaLoaderTask", "Using fallback skin path: " + texturePath);

                            }



                            InputStream stream = null;

                            try {

                                if (isFallback || texturePath.startsWith("android://")) {

                                    stream = ContentUtils.getInputStream(new URI(texturePath));

                                } else {

                                    stream = ContentUtils.getInputStream(texturePath);

                                }



                                if (stream != null) {

                                    Bitmap originalSkin = BitmapFactory.decodeStream(stream);



                                    if (originalSkin != null) {

                                        Log.i("ColladaLoaderTask", "Generating Roblox map for: " + texturePath);



                                        // Generate mapped texture

                                        Bitmap mappedTexture = generateFullRobloxTexture(originalSkin, originalSkin);



                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                        mappedTexture.compress(Bitmap.CompressFormat.PNG, 100, baos);

                                        byte[] textureBytes = baos.toByteArray();



                                        element.getMaterial().getColorTexture().setData(textureBytes);

                                        element.getMaterial().getColorTexture().setFile(texturePath);

                                        element.getMaterial().setAlpha(1.0f); // Force visible



                                        Log.i("ColladaLoaderTask", "Success! Skin applied (" + textureBytes.length + " bytes)");



                                        originalSkin.recycle();

                                        mappedTexture.recycle();

                                    } else {

                                        Log.e("ColladaLoaderTask", "Failed to decode bitmap: " + texturePath);

                                    }

                                    stream.close();

                                }

                            } catch (Exception ex) {

                                Log.e("ColladaLoaderTask", "Error reading/processing skin: " + ex.getMessage());

                                if (stream != null) try { stream.close(); } catch(Exception ignored){}

                            }

                        }

                    }

                }

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error loading materials", ex);

            }



            // Skinning Data

            Log.i("ColladaLoaderTask", "Loading skinning data...");

            Map<String, SkinningData> skins = null;

            try {

                XmlNode library_controllers = xml.getChild("library_controllers");

                if (library_controllers != null && !library_controllers.getChildren("controller").isEmpty()) {

                    SkinLoader skinLoader = new SkinLoader(library_controllers, Constants.MAX_VERTEX_WEIGHTS);

                    skins = skinLoader.loadSkinData();

                    for (int i = 0; i < allMeshes.size(); i++) {

                        SkinningData skinningData = skins.get(allMeshes.get(i).getId());

                        if (skinningData != null) {

                            final MeshData meshData = allMeshes.get(i);

                            final AnimatedModel data3D = (AnimatedModel) ret.get(i);

                            meshData.setBindShapeMatrix(skinningData.getBindShapeMatrix());

                            data3D.setBindShapeMatrix(meshData.getBindShapeMatrix());

                        }

                    }

                }

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error loading skinning data", ex);

            }



            final AnimationLoader loader = new AnimationLoader(xml);



            try {

                if (loader.isAnimated()) {

                    Log.i("ColladaLoaderTask", "Loading joints...");

                    SkeletonLoader skeletonLoader = new SkeletonLoader(xml);

                    skeletonLoader.updateJointData(skins, skeletons);



                    for (int i = 0; i < allMeshes.size(); i++) {

                        final MeshData meshData = allMeshes.get(i);

                        final AnimatedModel data3D = (AnimatedModel) ret.get(i);

                        SkeletonData skeletonData = skeletons.get(meshData.getId());

                        if (skeletonData == null) skeletonData = skeletons.get("default");



                        SkinLoader.loadSkinningData(meshData, skins != null? skins.get(meshData.getId()) : null, skeletonData);

                        SkinLoader.loadSkinningArrays(meshData);



                        data3D.setJoints(meshData.getJointsBuffer());

                        data3D.setWeights(meshData.getWeightsBuffer());

                    }

                }

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error updating joint data", ex);

            }



            try {

                if (loader.isAnimated()) {

                    Log.i("ColladaLoaderTask", "Loading animation...");

                    final Animation animation = loader.load();

                    for (int i = 0; i < allMeshes.size(); i++) {

                        final MeshData meshData = allMeshes.get(i);

                        final AnimatedModel data3D = (AnimatedModel) ret.get(i);

                        SkeletonData skeletonData = skeletons.get(meshData.getId());

                        if (skeletonData == null) skeletonData = skeletons.get("default");



                        data3D.setSkeleton(skeletonData);

                        data3D.setAnimation(animation);

                        data3D.setBindTransform(null);

                    }

                }

            } catch (Exception ex) {

                Log.e("ColladaLoaderTask", "Error loading animation", ex);

            }



            Log.i("ColladaLoaderTask", "Loading model finished. Objects: " + ret.size());



        } catch (Exception ex) {

            Log.e("ColladaLoaderTask", "Problem loading model", ex);

        }

        return ret;

    }



    // =========================================================================

    //  ROBLOX TEXTURE GENERATION LOGIC

    // =========================================================================



    private static Bitmap generateFullRobloxTexture(Bitmap pantsBitmap, Bitmap shirtBitmap) {

        Bitmap finalTexture = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(finalTexture);



        // Fill background with white to fix "invisible model" issue

        canvas.drawColor(Color.WHITE);



        int i = 1; // Scale factor matches RobloxSkinKt default

        Side[] sides = Side.values();



        // 1. Draw PANTS

        Part[] pantsParts = {Part.BODY, Part.LEFT_LEG, Part.RIGHT_LEG};

        for (Part part : pantsParts) {

            for (Side side : sides) {

                Rect srcRect = getSourceRect(part, side, i);

                Rect dstRect = getDestRect(part, side, i);

                if (srcRect.width() > 0 && dstRect.width() > 0 && pantsBitmap != null) {

                    drawPart(canvas, pantsBitmap, srcRect, dstRect);

                }

            }

        }



        // 2. Draw SHIRT (On top)

        Part[] shirtParts = {Part.BODY, Part.LEFT_ARM, Part.RIGHT_ARM};

        for (Part part : shirtParts) {

            for (Side side : sides) {

                Rect srcRect = getSourceRect(part, side, i);

                Rect dstRect = getDestRect(part, side, i);

                if (srcRect.width() > 0 && dstRect.width() > 0 && shirtBitmap != null) {

                    drawPart(canvas, shirtBitmap, srcRect, dstRect);

                }

            }

        }

        return finalTexture;

    }



    private static void drawPart(Canvas canvas, Bitmap source, Rect src, Rect dst) {

        try {

            if (src.left >= 0 && src.top >= 0 && src.right <= source.getWidth() && src.bottom <= source.getHeight()) {

                Bitmap piece = Bitmap.createBitmap(source, src.left, src.top, src.width(), src.height());

                canvas.drawBitmap(piece, null, dst, null);

            }

        } catch (Exception e) {}

    }



    private static Rect getSourceRect(Part part, Side side, int i) {

        if (part == Part.BODY) {

            if (side == Side.FRONT) return new Rect(i * 229, i * 72, i * 229 + (i * 132), i * 72 + (i * 132));

            if (side == Side.LEFT)  return new Rect(i * 361, i * 72, i * 361 + (i * 66), i * 72 + (i * 132));

            if (side == Side.BACK)  return new Rect(i * 427, i * 72, i * 427 + (i * 129), i * 72 + (i * 132));

            if (side == Side.RIGHT) return new Rect(i * 165, i * 72, i * 165 + (i * 64), i * 72 + (i * 132));

            if (side == Side.UP)    return new Rect(i * 229, i * 8,  i * 229 + (i * 32), i * 8 + (i * 64));

            if (side == Side.DOWN)  return new Rect(i * 229, i * 204, i * 229 + (i * 132), i * 204 + (i * 64));

        }



        if (part == Part.RIGHT_ARM || part == Part.RIGHT_LEG) {

            if (side == Side.FRONT) return new Rect(i * 215, i * 353, i * 215 + (i * 64), i * 353 + (i * 132));

            if (side == Side.LEFT)  return new Rect(i * 19,  i * 353, i * 19 + (i * 66),  i * 353 + (i * 132));

            if (side == Side.BACK)  return new Rect(i * 85,  i * 353, i * 85 + (i * 64),  i * 353 + (i * 132));

            if (side == Side.RIGHT) return new Rect(i * 149, i * 353, i * 149 + (i * 68), i * 353 + (i * 132));

            if (side == Side.UP)    return new Rect(i * 215, i * 289, i * 215 + (i * 68), i * 289 + (i * 64));

            if (side == Side.DOWN)  return new Rect(i * 215, i * 485, i * 215 + (i * 68), i * 485 + (i * 64));

        }



        if (part == Part.LEFT_ARM || part == Part.LEFT_LEG) {

            if (side == Side.FRONT) return new Rect(i * 307, i * 353, i * 307 + (i * 64), i * 353 + (i * 132));

            if (side == Side.LEFT)  return new Rect(i * 375, i * 353, i * 375 + (i * 66), i * 353 + (i * 132));

            if (side == Side.BACK)  return new Rect(i * 441, i * 353, i * 441 + (i * 64), i * 353 + (i * 132));

            if (side == Side.RIGHT) return new Rect(i * 505, i * 353, i * 505 + (i * 68), i * 353 + (i * 132));

            if (side == Side.UP)    return new Rect(i * 307, i * 289, i * 307 + (i * 68), i * 289 + (i * 64));

            if (side == Side.DOWN)  return new Rect(i * 307, i * 485, i * 307 + (i * 68), i * 485 + (i * 64));

        }

        return new Rect(0, 0, 0, 0);

    }



    private static Rect getDestRect(Part part, Side side, int i) {

        // Exact translation of RobloxSkinKt.frameIn for the 'textture' template case

        if (part == Part.BODY) {

            if (side == Side.FRONT) return new Rect(0, i * 72, i * 132, i * 72 + (i * 132));

            if (side == Side.LEFT)  return new Rect(i * 132, i * 72, i * 132 + (i * 66), i * 72 + (i * 132));

            if (side == Side.BACK)  return new Rect(i * 198, i * 72, i * 198 + (i * 129), i * 72 + (i * 132));

            if (side == Side.RIGHT) return new Rect(i * 327, i * 72, i * 327 + (i * 64), i * 72 + (i * 132));

            if (side == Side.UP)    return new Rect(0, i * 8, i * 132, i * 8 + (i * 64));

            if (side == Side.DOWN)  return new Rect(0, i * 204, i * 132, i * 204 + (i * 64));

        }



        if (part == Part.LEFT_ARM) {

            if (side == Side.FRONT) return new Rect(i * 564, i * 64, i * 564 + (i * 64), i * 64 + (i * 112));

            if (side == Side.LEFT)  return new Rect(i * 628, i * 64, i * 628 + (i * 66), i * 64 + (i * 112));

            if (side == Side.BACK)  return new Rect(i * 694, i * 64, i * 694 + (i * 64), i * 64 + (i * 112));

            if (side == Side.RIGHT) return new Rect(i * 496, i * 64, i * 496 + (i * 68), i * 64 + (i * 112));

            if (side == Side.UP)    return new Rect(i * 496, 0, i * 496 + (i * 68), i * 64);

            if (side == Side.DOWN)  return new Rect(i * 692, i * 215, i * 692 + (i * 68), i * 215 + (i * 64));

        }



        if (part == Part.RIGHT_ARM) {

            if (side == Side.FRONT) return new Rect(i * 828, i * 64, i * 828 + (i * 64), i * 64 + (i * 112));

            if (side == Side.LEFT)  return new Rect(i * 892, i * 64, i * 892 + (i * 66), i * 64 + (i * 112));

            if (side == Side.BACK)  return new Rect(i * 958, i * 64, i * 958 + (i * 64), i * 64 + (i * 112));

            if (side == Side.RIGHT) return new Rect(i * 760, i * 64, i * 760 + (i * 68), i * 64 + (i * 112));

            if (side == Side.UP)    return new Rect(i * 760, 0, i * 760 + (i * 68), i * 64);

            if (side == Side.DOWN)  return new Rect(i * 956, i * 215, i * 956 + (i * 68), i * 215 + (i * 64));

        }



        if (part == Part.LEFT_LEG) {

            if (side == Side.FRONT) return new Rect(i * 564, i * 348, i * 564 + (i * 64), i * 348 + (i * 112));

            if (side == Side.LEFT)  return new Rect(i * 628, i * 348, i * 628 + (i * 66), i * 348 + (i * 112));

            if (side == Side.BACK)  return new Rect(i * 694, i * 348, i * 694 + (i * 64), i * 348 + (i * 112));

            if (side == Side.RIGHT) return new Rect(i * 496, i * 348, i * 496 + (i * 68), i * 348 + (i * 112));

            if (side == Side.UP)    return new Rect(i * 496, i * 284, i * 496 + (i * 68), i * 284 + (i * 64));

            if (side == Side.DOWN)  return new Rect(i * 692, i * 499, i * 692 + (i * 68), i * 499 + (i * 64));

        }



        if (part == Part.RIGHT_LEG) {

            if (side == Side.FRONT) return new Rect(i * 828, i * 348, i * 828 + (i * 64), i * 348 + (i * 112));

            if (side == Side.LEFT)  return new Rect(i * 892, i * 348, i * 892 + (i * 66), i * 348 + (i * 112));

            if (side == Side.BACK)  return new Rect(i * 958, i * 348, i * 958 + (i * 64), i * 348 + (i * 112));

            if (side == Side.RIGHT) return new Rect(i * 760, i * 348, i * 760 + (i * 68), i * 348 + (i * 112));

            if (side == Side.UP)    return new Rect(i * 760, i * 284, i * 760 + (i * 68), i * 284 + (i * 64));

            if (side == Side.DOWN)  return new Rect(i * 956, i * 499, i * 956 + (i * 68), i * 499 + (i * 64));

        }

        return new Rect(0, 0, 0, 0);

    }

}