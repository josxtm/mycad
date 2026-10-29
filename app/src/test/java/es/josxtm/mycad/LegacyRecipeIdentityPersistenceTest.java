package es.josxtm.mycad;

import static org.junit.Assert.*;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import es.josxtm.mycad.cad.BoxFaceSelection;
import es.josxtm.mycad.cad.DrilledBoxRecipe;
import es.josxtm.mycad.cad.FaceUnionRecipe;

/** Real Android project JSON round trips, including files written before recipe UUIDs existed. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class LegacyRecipeIdentityPersistenceTest {
 private MainActivity activity() {
  return Robolectric.buildActivity(MainActivity.class).setup().get();
 }
 private MainActivity prepared() {
  MainActivity a=activity();
  DrilledBoxRecipe drill=new DrilledBoxRecipe(0,0,0,20,20,20,0,0,4);
  FaceUnionRecipe union=new FaceUnionRecipe(20,20,20,0,0,0,BoxFaceSelection.Face.X_MAX,5);
  a.booleanMeshes.add(new MeshEngine.Mesh(drill.regenerate()));
  a.booleanMeshes.add(new MeshEngine.Mesh(new float[0]));
  a.holeRecipes.add(drill);a.holeRecipes.add(null);
  a.unionRecipes.add(null);a.unionRecipes.add(union);
  a.loopExtrusionFeatures.add(null);a.loopExtrusionFeatures.add(null);
  a.intersectionRecipes.add(null);a.intersectionRecipes.add(null);
  return a;
 }
 @Test public void saveAndReloadRetainBothFeatureIdentities() throws Exception {
  MainActivity a=prepared();
  String drillId=a.holeRecipes.get(0).id,unionId=a.unionRecipes.get(1).id;
  JSONObject json=new JSONObject(a.encode());
  assertEquals(drillId,json.getJSONArray("throughHoleRecipes").getJSONObject(0).getString("id"));
  assertEquals(unionId,json.getJSONArray("faceUnionRecipes").getJSONObject(1).getString("id"));
  MainActivity.ParsedDesign restored=a.decode(json.toString());
  assertEquals(drillId,restored.drills.get(0).id);
  assertEquals(unionId,restored.recipes.get(1).id);
  assertEquals(unionId,restored.recipes.get(1).withDistance(8).id);
  assertEquals(drillId,restored.drills.get(0).withHole(1,0,3).id);
 }
 @Test public void oldProjectsWithoutIdsStillLoad() throws Exception {
  MainActivity a=prepared();
  JSONObject old=new JSONObject(a.encode());
  old.getJSONArray("throughHoleRecipes").getJSONObject(0).remove("id");
  old.getJSONArray("faceUnionRecipes").getJSONObject(1).remove("id");
  MainActivity.ParsedDesign restored=a.decode(old.toString());
  assertNotNull(java.util.UUID.fromString(restored.drills.get(0).id));
  assertNotNull(java.util.UUID.fromString(restored.recipes.get(1).id));
  assertNotEquals(restored.drills.get(0).id,restored.recipes.get(1).id);
 }
 @Test public void malformedStoredIdIsRejected() throws Exception {
  MainActivity a=prepared();
  JSONObject bad=new JSONObject(a.encode());
  bad.getJSONArray("throughHoleRecipes").getJSONObject(0).put("id","bad");
  try {a.decode(bad.toString());fail("Invalid UUID accepted");}
  catch(IOException expected) {assertTrue(expected.getMessage().contains("Perforación"));}
  bad=new JSONObject(a.encode());
  bad.getJSONArray("faceUnionRecipes").getJSONObject(1).put("id","bad");
  try {a.decode(bad.toString());fail("Invalid UUID accepted");}
  catch(IOException expected) {assertTrue(expected.getMessage().contains("unión"));}
 }
}
