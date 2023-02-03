package cx.shapefile.interfaces;

import org.opengis.referencing.operation.MathTransform;

public interface ProjectTransfer
{
    void projectShape(String inputShp, String outputShp, String endEPSG) throws Exception;

    MathTransform toProject(String startEPSG , String endEPSG) throws Exception;

    Double[] transform(double x, double y , String startEPSG, String endEPSG) throws Exception;
}
