import es.josxtm.mycad.cad.TouchDragThreshold;

public final class TouchDragThresholdTest {
 static void eq(boolean actual,boolean expected,String label){
  if(actual!=expected)throw new AssertionError(label);
 }
 static void reject(Runnable r,String label){
  try{r.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("Bad threshold accepted: "+label);
 }
 public static void main(String[] args){
  eq(TouchDragThreshold.moved(false,100,200,100,200,12),false,"Exact tap");
  eq(TouchDragThreshold.moved(false,100,200,111,200,12),false,"Small jitter");
  eq(TouchDragThreshold.moved(false,100,200,112,200,12),false,"Threshold boundary");
  eq(TouchDragThreshold.moved(false,100,200,113,200,12),true,"Intentional drag");
  eq(TouchDragThreshold.moved(false,100,200,109,209,12),true,"Diagonal movement");
  eq(TouchDragThreshold.moved(true,100,200,100,200,12),true,"Dragging remains sticky");
  eq(TouchDragThreshold.moved(false,100,200,111,200,24),false,"High DPI threshold");
  eq(TouchDragThreshold.moved(false,-200,-100,-250,-120,12),true,"Negative coords");
  reject(()->TouchDragThreshold.moved(false,0,0,1,1,0),"Zero radius");
  reject(()->TouchDragThreshold.moved(false,0,0,1,1,-2),"Negative radius");
  reject(()->TouchDragThreshold.moved(false,0,0,Double.NaN,1,12),"NaN coordinate");
  reject(()->TouchDragThreshold.moved(false,0,0,1,1,Double.POSITIVE_INFINITY),
    "Infinite threshold");
  System.out.println("TouchDragThresholdTest: OK");
 }
}
