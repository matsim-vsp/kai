package playground.agarwalamit.cadyts.marginals;

import java.util.Map;
import javax.inject.Inject;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import org.matsim.api.core.v01.Id;

/*package*/ class ModalDistanceSimResultsContainerImpl implements SimResults<ModalBin> {

    private static final long serialVersionUID = 1L;

    @Inject
    private BeelineDistanceCollector beelineDistanceCollector;
    private final double countsScaleFactor;

    ModalDistanceSimResultsContainerImpl(final double countsScaleFactor) {
        this.countsScaleFactor = countsScaleFactor;
    }

    @Override
    public double getSimValue(final ModalBin modalBin, final int low, final int high, final TYPE type) {

        return this.beelineDistanceCollector.getOutputDistanceDistribution().getModalBinToDistanceBin().get(modalBin.getId()).getCount();
    }

    @Override
    public String toString() {
        final String MODE = "mode: ";
        final String distanceRange = "; distanceRange: ";
        final String VALUES = "; values:";
        final char TAB = '\t';
        final char RETURN = '\n';

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(MODE+TAB);
        stringBuffer.append(distanceRange+TAB);
        stringBuffer.append(VALUES+RETURN);

        DistanceDistribution distanceDistribution = this.beelineDistanceCollector.getOutputDistanceDistribution();
        for (Map.Entry<Id<ModalBin>, DistanceBin> entry : distanceDistribution.getModalBinToDistanceBin().entrySet()) {
            if (entry.getValue().getCount() > 0) {
                    stringBuffer.append(entry.getKey().toString()+TAB);
                    stringBuffer.append(entry.getValue().getCount()+RETURN);
            }
        }
        return stringBuffer.toString();
    }

}