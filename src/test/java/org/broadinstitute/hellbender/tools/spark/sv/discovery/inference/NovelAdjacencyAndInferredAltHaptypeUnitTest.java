package org.broadinstitute.hellbender.tools.spark.sv.discovery.inference;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import htsjdk.samtools.TextCigarCodec;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.tools.spark.sv.SVTestUtils;
import org.broadinstitute.hellbender.tools.spark.sv.StructuralVariationDiscoveryArgumentCollection;
import org.broadinstitute.hellbender.tools.spark.sv.discovery.SVDiscoveryTestDataProvider;
import org.broadinstitute.hellbender.tools.spark.sv.discovery.alignment.AlignedContig;
import org.broadinstitute.hellbender.tools.spark.sv.discovery.alignment.AlignmentInterval;
import org.broadinstitute.hellbender.tools.spark.sv.discovery.alignment.ContigAlignmentsModifier;
import org.broadinstitute.hellbender.tools.spark.sv.discovery.alignment.StrandSwitch;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.Tuple2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class NovelAdjacencyAndInferredAltHaptypeUnitTest extends GATKBaseTest {


    /**
     * Hack to force trigger test data generation.
     */
    @BeforeClass
    private void makeSureDataIsAvailable() {
        if(!SVDiscoveryTestDataProvider.testDataInitialized) {
            new SVDiscoveryTestDataProvider();
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Tests for generic functions on the base class (uses inversion subclass for testing)
    // -----------------------------------------------------------------------------------------------
    @Test(groups = "sv")
    public void testEqualsAndHashCode() {

        final NovelAdjacencyAndInferredAltHaptype novelAdjacencyAndInferredAltHaptype1 = getBreakpoints("asm00001:tig0001", "foo");

        final NovelAdjacencyAndInferredAltHaptype novelAdjacencyAndInferredAltHaptype2 = getBreakpoints("asm00002:tig0002", "bar");

        Assert.assertEquals(novelAdjacencyAndInferredAltHaptype1, novelAdjacencyAndInferredAltHaptype2);
        Assert.assertEquals(novelAdjacencyAndInferredAltHaptype1.hashCode(), novelAdjacencyAndInferredAltHaptype2.hashCode());
    }

    @Test(groups = "sv")
    void testKryoSerializer() throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final Output out = new Output(bos);
            final Kryo kryo = new Kryo();
            final NovelAdjacencyAndInferredAltHaptype novelAdjacencyAndInferredAltHaptype =
                    getBreakpoints("asm00001:tig0001", "foo");
            kryo.writeClassAndObject(out, novelAdjacencyAndInferredAltHaptype);
            out.flush();

            try ( final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray()) ) {
                final Input in = new Input(bis);
                @SuppressWarnings("unchecked")
                final NovelAdjacencyAndInferredAltHaptype roundTrip = (NovelAdjacencyAndInferredAltHaptype) kryo.readClassAndObject(in);
                Assert.assertEquals(roundTrip, novelAdjacencyAndInferredAltHaptype);
            }
        }
    }

    private static NovelAdjacencyAndInferredAltHaptype getBreakpoints(final String contigName, final String insertionMapping) {
        final AlignmentInterval region1 = new AlignmentInterval(new SimpleInterval("20", 10000, 10100), 1, 100, TextCigarCodec.decode("100M"), true, 60, 0, 100, ContigAlignmentsModifier.AlnModType.NONE);
        final AlignmentInterval region2 = new AlignmentInterval(new SimpleInterval("20", 20100, 20200), 101, 200, TextCigarCodec.decode("100M"), false, 60, 0, 100, ContigAlignmentsModifier.AlnModType.NONE);
        final ArrayList<String> insertionMappings = new ArrayList<>();
        insertionMappings.add(insertionMapping);
        final ChimericAlignment breakpoint = new ChimericAlignment(region1, region2, insertionMappings, contigName, SVDiscoveryTestDataProvider.b37_seqDict);
        return new NovelAdjacencyAndInferredAltHaptype(breakpoint, SVTestUtils.makeDummySequence(200, (byte)'A'), SVDiscoveryTestDataProvider.b37_seqDict);
    }

    // -----------------------------------------------------------------------------------------------
    // Tests for inversion
    // -----------------------------------------------------------------------------------------------
    @Test(groups = "sv")
    public void testGetBreakpoints_5to3Inversion_withSimpleInsertion() {

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forSimpleInversionWithNovelInsertion.biPathBubble;
        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.FORWARD_TO_REVERSE,
                new SimpleInterval("21", 69294, 69294), new SimpleInterval("21", 69364, 69364),
                null, "", "T", 0, 0, null,
                new byte[]{'T'});
    }

    @Test(groups = "sv")
    public void testGetAssembledBreakpointFromAlignmentIntervalsStrangeLeftBreakpoint() {

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forSimpleInversionFromLongCtg1WithStrangeLeftBreakpoint.biPathBubble;
        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.REVERSE_TO_FORWARD,
                new SimpleInterval(SVDiscoveryTestDataProvider.chrForLongContig1, 20138006, 20138006),
                new SimpleInterval(SVDiscoveryTestDataProvider.chrForLongContig1, 20152650, 20152650),
                null, "TGAGGTCAGGAGTTCCTGATCCCATCTTTACTAAAAATACAAAACTTACCCAGGGTGGTTGTGCACACTTGTAATCCCAGCTACTTGGGAGGCTGAGGCAGGAGAATTGCTTGAACAAGGGAGGAAATGGTTGCAGTGAGCCATGATCATGCCACTGAACCCCAGCCTGGGCAAGAGAGTGAGACTGTCTCAAAAAAAAAAAAAACTGTTTAATTTTTATGAATGCAGGTTTTCTGCAAACACTACACATAACTATGCTAATTGTTCTGAAGTAATAAATAGAAAGCAAGGCACAACTACAGACTCCACTGTTCAGTTTATGCACTGAACTGTTCTTGCTTTTGCAGTGTAAGTATTTCTGCCTGCAAATACTGGATAATTACCTTGGATCATCAGATTTCTATCAAAGGAATTTAGTATCTTTTAGTCTTTATCATTTTGTATTGCTAAATTTATCTGTGTGTTAAGCTTCTGTGTGCTCTTAAAATGAGGTTTTATCTAAACAAACCTGTGTCTACTTTAAAAGACTAAACATGAAAAAACTAAACTTTTCAGAACCAAAAACAAAGCAATAAATCTGAAGTACTAGATAGTCTGGAGTGAGATTTATTTAGCTTTTTT",
                "", 0, 0, null,
                new byte[0]);
    }

    /**
     *  @see SVDiscoveryTestDataProvider#forSimpleInversionWithHomology(ByteArrayOutputStream)
     */
    @Test(groups = "sv")
    public void testSimpleInversionWithHomologyBreakpointsIdentification_allFourRepresentations() {

        final byte[] homology = "ACACA".getBytes();

        // left flanking forward strand
        final NovelAdjacencyAndInferredAltHaptype breakpointsIdentifiedFromLeftFlankingEvidenceAndForwardStrand = SVDiscoveryTestDataProvider.forSimpleInversionWithHom_leftPlus.biPathBubble;
        seeIfItWorksForNonSimpleTranslocations(breakpointsIdentifiedFromLeftFlankingEvidenceAndForwardStrand, StrandSwitch.FORWARD_TO_REVERSE,
                new SimpleInterval("20", 200, 200), new SimpleInterval("20", 605, 605),
                null, new String(homology), "", 0, 0, null,
                new byte[0]);

        // see if reverse strand changes anything
        Assert.assertEquals(breakpointsIdentifiedFromLeftFlankingEvidenceAndForwardStrand, SVDiscoveryTestDataProvider.forSimpleInversionWithHom_leftMinus.biPathBubble);

        // see if right flanking evidence give the same breakpoint location and homology (up to RC)
        // and see if the two strands give the same result
        final NovelAdjacencyAndInferredAltHaptype breakpointsIdentifiedFromRightFlankingEvidenceAndForwardStrand = SVDiscoveryTestDataProvider.forSimpleInversionWithHom_rightPlus.biPathBubble;
        Assert.assertEquals(breakpointsIdentifiedFromLeftFlankingEvidenceAndForwardStrand.getLeftJustifiedLeftRefLoc(),
                breakpointsIdentifiedFromRightFlankingEvidenceAndForwardStrand.getLeftJustifiedLeftRefLoc());
        Assert.assertEquals(breakpointsIdentifiedFromLeftFlankingEvidenceAndForwardStrand.getLeftJustifiedRightRefLoc(),
                breakpointsIdentifiedFromRightFlankingEvidenceAndForwardStrand.getLeftJustifiedRightRefLoc());
        Assert.assertEquals(breakpointsIdentifiedFromLeftFlankingEvidenceAndForwardStrand.getComplication().getHomologyForwardStrandRep(),
                new String(SVTestUtils.getReverseComplimentCopy(breakpointsIdentifiedFromRightFlankingEvidenceAndForwardStrand.getComplication().getHomologyForwardStrandRep().getBytes())));
        Assert.assertEquals(breakpointsIdentifiedFromRightFlankingEvidenceAndForwardStrand.getStrandSwitch(), StrandSwitch.REVERSE_TO_FORWARD);
        Assert.assertEquals(breakpointsIdentifiedFromRightFlankingEvidenceAndForwardStrand, SVDiscoveryTestDataProvider.forSimpleInversionWithHom_rightMinus.biPathBubble);
    }

    @Test(groups = "sv")
    public void testGetAssembledBreakpointsFromAlignmentIntervalsWithOverlappingAlignmentInterval() {
        final byte[] contigSequence = "ACTAGAGCATCTACGTGTTCCTGTGGTTTTGGAGCAAGAGTGATTTGAGTTTCAGAGATTTTTACTAATTCTTCTTCCCCTACCAGAAAAAAAGATCTTACCATTTGAGAGTGAGATGTAAACCCAGCCCTGTCTGACCTGAGTCTGTGCCCTAAGCCTATGCTAAGCCAAGCAGTGCCTGGAGCCACCACAGGTCCACACAATTCGTTAACATGATGAAGCAAGGATGGAAATTGGACAAAATAGTGTGCCTACTGAATCTAAGAATGAAAAATGATTGCACTCCTACTCTGAGTGCTTTGGAGCACTGCCCAGTTGGGCAAAGGGTCAGCGCCTGGGCAGAGGTCCCCACAACCTGGCAGGAGTGTGGTCGGCCACCCTATGGGCCTCCATCATGTGCAGTGACAGCGGGGCTGTCATGTCACCGTGTGGGAGGGCTTGCAGGTGAAGTGGTCTGGGAGGGGTCCCCCAGACAAAGCCAAGGTTCTGAGAGTTGGCCCGAACACTGCTGGATTCCACTTCACCTGCAAGCCCTCCCACACGGTGACATGACAGCCTATAATACAGTTCCGCATGGCCACGTCATACAACCCTGTCATATTGGTGAGCAATTGCTGTGTAGCCAAAGACCCCAAAACTCAAACAGCATTTATTATTATTGCCCCCATGTCTGAGAGTCAGATGTGCATTTGCTGATCTCAGCTTGTTTGAGCTGCTGCAGGGTTGGGGCTCTGCTCCAGGCAGGCTTAGCTGTCACCACATGCACACATACATTCTGGGCCTCTGCTGCGCGCGTCACGTTCACTGAAGATCTTGGGATTGGGAGTTAGGGCGGTGGGAGGGCCCAGCAAAGTCACCTGGCGATGGCAGGGACACAGGGAGGAATGTAGAATGGGGCCGATGATGGGACCCACACGTCTGCAAAGCTGCGGTCTCCTTGAGGGGTGGAGACAGCAACAACTCACCGCACGCGGTGCTTCAGTTCACCATCTCCCTGGGACATTAGGGGGCCCCGTGTTATCTCATTTTGCTCTGGTTTGCATTAGTTTTTTATCACTTCGTAGATGAAGCCACTGACACCCAGAGAGGGAAAGTGGCCTGACCAAGGGCCACAGCAGGGGAGCGAAGGAGCCCCACAGTTCGGCAGGAACACAGCCTCTCCCTGGCTTTCAGGTTCACTGACATCTTCTCATGGCCTCTGTAACTCACCAGGCATCAGGGTGTAGTCCTTAGACCAGTGTCCCACAGCTGCCACAGAGTGGGAGCTCACCATCAGTTATAAGTCACTAGAAAGGCTTTTGGACATTATAAGCTACAATGGAAAATAAGTCATCTGTGGATTTTTGTGACAGATTCCAAAAATTTGAATATTTTGTCTACTTAGGTTTTTGGTTAATTTTATCCTCAAAACTGTTCTGCAGTGATTAAGCTGTACAAACTGCATCATGGGCGAATTGGCATATTCAGAAATGACTGATATTCTTGATTTCAGTTTTTTACTTTGTATGTAGCTCCTCAAGGAAAC".getBytes();
        final AlignmentInterval region1 = new AlignmentInterval(new SimpleInterval("20", 23102785, 23103304), 1, 519, TextCigarCodec.decode("519M1006S"), true, 60, 1, 100, ContigAlignmentsModifier.AlnModType.NONE);
        final AlignmentInterval region2 = new AlignmentInterval(new SimpleInterval("20", 23103196, 23103238), 516, 557, TextCigarCodec.decode("515S42M968S"), false, 60, 2, 100, ContigAlignmentsModifier.AlnModType.NONE);
        final AlignmentInterval region3 = new AlignmentInterval(new SimpleInterval("20", 23103633, 23104603), 556, 1525, TextCigarCodec.decode("555S970M"), true, 60, 3, 100, ContigAlignmentsModifier.AlnModType.NONE);

        final AlignedContig alignedContig = new AlignedContig("asm00001:tig0001", contigSequence, Arrays.asList(region1, region2, region3), false);
        final List<ChimericAlignment> assembledBreakpointsFromAlignmentIntervals = ChimericAlignment.parseOneContig(alignedContig, SVDiscoveryTestDataProvider.b37_seqDict, true, StructuralVariationDiscoveryArgumentCollection.DiscoverVariantsFromContigsAlignmentsSparkArgumentCollection.DEFAULT_MIN_ALIGNMENT_LENGTH, StructuralVariationDiscoveryArgumentCollection.DiscoverVariantsFromContigsAlignmentsSparkArgumentCollection.CHIMERIC_ALIGNMENTS_HIGHMQ_THRESHOLD, true);
        Assert.assertEquals(assembledBreakpointsFromAlignmentIntervals.size(), 1);
        final ChimericAlignment chimericAlignment = assembledBreakpointsFromAlignmentIntervals.get(0);
        Assert.assertEquals(chimericAlignment.sourceContigName, "asm00001:tig0001");
        Assert.assertEquals(chimericAlignment.regionWithLowerCoordOnContig, region1);
        Assert.assertEquals(chimericAlignment.regionWithHigherCoordOnContig, region3);
        Assert.assertEquals(chimericAlignment.insertionMappings.size(), 1);
        final String expectedInsertionMappingsString = String.join(AlignmentInterval.PACKED_STRING_REP_SEPARATOR, "516", "557", "20:23103196-23103238", "-", "515S42M968S", "60", "2", "100", "O");
        Assert.assertEquals(chimericAlignment.insertionMappings.get(0), expectedInsertionMappingsString);
        final NovelAdjacencyAndInferredAltHaptype breakpoints = new NovelAdjacencyAndInferredAltHaptype(chimericAlignment, contigSequence, SVDiscoveryTestDataProvider.b37_seqDict);
        Assert.assertTrue(breakpoints.getComplication().getHomologyForwardStrandRep().isEmpty());
        Assert.assertEquals(breakpoints.getComplication().getInsertedSequenceForwardStrandRep().getBytes(), Arrays.copyOfRange(contigSequence, 519, 555));
        Assert.assertEquals(breakpoints.getAltHaplotypeSequence(), Arrays.copyOfRange(contigSequence, 519, 555));
    }

    // following might be legacy tests that could be removed but needs time to investigate (Dec.13/2016)
    @Test(groups = "sv")
    public void testGetBreakpoints_5to3Inversion_simple() {
        final AlignmentInterval region1 = new AlignmentInterval(new SimpleInterval("20", 101, 200), 1, 100, TextCigarCodec.decode("100M100S"), true, 60, 0, 100, ContigAlignmentsModifier.AlnModType.NONE);
        final AlignmentInterval region2 = new AlignmentInterval(new SimpleInterval("20", 501, 600), 101, 200, TextCigarCodec.decode("100S100M"), false, 60, 0, 100, ContigAlignmentsModifier.AlnModType.NONE);
        final ChimericAlignment chimericAlignment = new ChimericAlignment(region1, region2, Collections.emptyList(), "1", SVDiscoveryTestDataProvider.b37_seqDict);
        final Tuple2<SimpleInterval, SimpleInterval> breakpoints =
                BreakpointsInference.getInferenceClass(chimericAlignment, null, SVDiscoveryTestDataProvider.b37_seqDict)
                .getLeftJustifiedBreakpoints();
        Assert.assertEquals(breakpoints._1(), new SimpleInterval("20", 200, 200));
        Assert.assertEquals(breakpoints._2(), new SimpleInterval("20", 600, 600));
    }

    @Test(groups = "sv")
    public void testGetBreakpoints_5to3Inversion_withSimpleHomology() {
        final byte[] contigSeq = StringUtils.repeat("C", 50).concat("ATATAT").concat(StringUtils.repeat("C", 50)).getBytes();

        final AlignmentInterval region1 = new AlignmentInterval(new SimpleInterval("20", 101, 156), 1, 56, TextCigarCodec.decode("56M50S"), true, 60, 0, 56, ContigAlignmentsModifier.AlnModType.NONE);
        final AlignmentInterval region2 = new AlignmentInterval(new SimpleInterval("20", 501, 556), 51, 106, TextCigarCodec.decode("56M50S"), false, 60, 0, 56, ContigAlignmentsModifier.AlnModType.NONE);
        final ChimericAlignment chimericAlignment = new ChimericAlignment(region1, region2, Collections.emptyList(), "1", SVDiscoveryTestDataProvider.b37_seqDict);

        final Tuple2<SimpleInterval, SimpleInterval> breakpoints =
                BreakpointsInference.getInferenceClass(chimericAlignment, contigSeq, SVDiscoveryTestDataProvider.b37_seqDict)
                        .getLeftJustifiedBreakpoints();
        Assert.assertEquals(breakpoints._1(), new SimpleInterval("20", 150, 150));
        Assert.assertEquals(breakpoints._2(), new SimpleInterval("20", 556, 556));
    }

    // -----------------------------------------------------------------------------------------------
    // Tests for complication resolving and breakpoint justification with the inferred complications for insertion and deletion
    // -----------------------------------------------------------------------------------------------

    /**
     * @see SVDiscoveryTestDataProvider#forSimpleDeletion(ByteArrayOutputStream)
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_simpleDeletion() {

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forSimpleDeletion_plus.biPathBubble;
        final NovelAdjacencyAndInferredAltHaptype breakpointsDetectedFromReverseStrand = SVDiscoveryTestDataProvider.forSimpleDeletion_minus.biPathBubble;

        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.NO_SWITCH, new SimpleInterval("21", 100040, 100040), new SimpleInterval("21", 100060, 100060),
                null, "", "", 0, 0, null,
                new byte[0]);
        Assert.assertEquals(breakpointsDetectedFromReverseStrand, breakpoints);
    }

    /**
     * @see SVDiscoveryTestDataProvider#forSimpleInsertion(ByteArrayOutputStream)
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_simpleInsertion() {

        final byte[] insertedSeq  = SVTestUtils.makeDummySequence(50, (byte)'C');

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forSimpleInsertion_plus.biPathBubble;
        final NovelAdjacencyAndInferredAltHaptype breakpointsDetectedFromReverseStrand = SVDiscoveryTestDataProvider.forSimpleInsertion_minus.biPathBubble;

        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.NO_SWITCH, new SimpleInterval("21", 100100, 100100), new SimpleInterval("21", 100100, 100100),
                null, "", new String(insertedSeq), 0, 0, null,
                insertedSeq);
        Assert.assertEquals(breakpointsDetectedFromReverseStrand, breakpoints);
    }

    /**
     * @see SVDiscoveryTestDataProvider#forLongRangeSubstitution()
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_longRangeSubstitution() {

        final byte[] substitution = SVTestUtils.makeDummySequence(10, (byte)'C');
        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forLongRangeSubstitution_plus.biPathBubble;
        final NovelAdjacencyAndInferredAltHaptype breakpointsDetectedFromReverseStrand = SVDiscoveryTestDataProvider.forLongRangeSubstitution_minus.biPathBubble;

        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.NO_SWITCH, new SimpleInterval("21", 100040, 100040), new SimpleInterval("21", 100060, 100060),
                null, "", new String(substitution), 0, 0, null,
                substitution);
        Assert.assertEquals(breakpointsDetectedFromReverseStrand, breakpoints);
    }

    /**
     * @see SVDiscoveryTestDataProvider#forDeletionWithHomology(ByteArrayOutputStream)
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_simpleDeletion_withHomology() {

        final byte[] homology = "ATCG".getBytes();

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forDeletionWithHomology_plus.biPathBubble;
        final NovelAdjacencyAndInferredAltHaptype breakpointsDetectedFromReverseStrand = SVDiscoveryTestDataProvider.forDeletionWithHomology_minus.biPathBubble;

        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.NO_SWITCH, new SimpleInterval("21", 100040, 100040), new SimpleInterval("21", 100078, 100078),
                null, new String(homology), "", 0, 0, null,
                new byte[0]);
        Assert.assertEquals(breakpointsDetectedFromReverseStrand, breakpoints);
    }

    /**
     * @see SVDiscoveryTestDataProvider#forSimpleTandemDuplicationContraction()
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_tandemDuplication_contraction_simple() {

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forSimpleTanDupContraction_plus.biPathBubble;
        final NovelAdjacencyAndInferredAltHaptype breakpointsDetectedFromReverseStrand = SVDiscoveryTestDataProvider.forSimpleTanDupContraction_minus.biPathBubble;

        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.NO_SWITCH, new SimpleInterval("21", 100040, 100040), new SimpleInterval("21", 100050, 100050),
                new SimpleInterval("21", 100041, 100050),
                new String(SVTestUtils.makeDummySequence(10, (byte)'C')), "",
                2, 1, Collections.emptyList(),
                SVTestUtils.makeDummySequence(10, (byte)'C'));
        Assert.assertEquals(breakpointsDetectedFromReverseStrand, breakpoints);
    }

    /**
     * @see SVDiscoveryTestDataProvider#forSimpleTandemDuplicationExpansion(ByteArrayOutputStream)
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_tandemDuplication_expansion_simple() {

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forSimpleTanDupExpansion_plus.biPathBubble;
        final NovelAdjacencyAndInferredAltHaptype breakpointsDetectedFromReverseStrand = SVDiscoveryTestDataProvider.forSimpleTanDupExpansion_minus.biPathBubble;

        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.NO_SWITCH, new SimpleInterval("21", 100040, 100040), new SimpleInterval("21", 100040, 100040),
                new SimpleInterval("21", 100041, 100050),
                "", "",
                1, 2, Arrays.asList("10M", "10M"),
                SVTestUtils.makeDummySequence(20, (byte)'C'));
        Assert.assertEquals(breakpointsDetectedFromReverseStrand, breakpoints);
    }

    /**
     * @see SVDiscoveryTestDataProvider#forSimpleTandemDuplicationExpansionWithNovelInsertion(ByteArrayOutputStream)
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_tandemDuplication_expansion_andNovelInsertion() {

        final String insertedSeq = "CTCTCTCTCT";                                                                           //10
        final String dup = "AAAAGTAAATGTTATAAGAAATCTTAAGTATTATTTTCTTATGTTTCTAGCCTAATAAAGTGCTTTTATTAAAGCACTTTATTTAAAGG";    //89
        final String alt = dup + insertedSeq + dup;

        final NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forSimpleTanDupExpansionWithNovelIns_plus.biPathBubble;
        final NovelAdjacencyAndInferredAltHaptype breakpointsDetectedFromReverseStrand = SVDiscoveryTestDataProvider.forSimpleTanDupExpansionWithNovelIns_minus.biPathBubble;

        seeIfItWorksForNonSimpleTranslocations(breakpoints, StrandSwitch.NO_SWITCH, new SimpleInterval("21", 25297163, 25297163), new SimpleInterval("21", 25297163, 25297163),
                new SimpleInterval("21", 25297164,25297252),
                "", insertedSeq,
                1, 2, Arrays.asList("89M", "89M"),
                alt.getBytes());
        Assert.assertEquals(breakpointsDetectedFromReverseStrand, breakpoints);
    }

    private static void seeIfItWorksForNonSimpleTranslocations(final NovelAdjacencyAndInferredAltHaptype breakpoints, final StrandSwitch expectedStrandSwitch,
                                                               final SimpleInterval expectedLeftBreakpoint, final SimpleInterval expectedRightBreakpoint,
                                                               final SimpleInterval expectedRepeatUnitRefSpan, final String expectedHomology, final String expectedInsertion,
                                                               final int expectedRefDupNum, final int expectedCtgDupNum,
                                                               final List<String> expectedTandupCigarStrings,
                                                               final byte[] expectedAltHaplotypeSequence) {

        Assert.assertEquals(breakpoints.getLeftJustifiedLeftRefLoc(), expectedLeftBreakpoint);
        Assert.assertEquals(breakpoints.getLeftJustifiedRightRefLoc(), expectedRightBreakpoint);
        Assert.assertEquals(breakpoints.getStrandSwitch(), expectedStrandSwitch);
        Assert.assertEquals(breakpoints.getAltHaplotypeSequence(), expectedAltHaplotypeSequence);
        if (expectedStrandSwitch.equals(StrandSwitch.NO_SWITCH)) {
            if (expectedRepeatUnitRefSpan == null) {
                final BreakpointComplications.SimpleInsDelOrReplacementBreakpointComplications complication =
                        (BreakpointComplications.SimpleInsDelOrReplacementBreakpointComplications) breakpoints.getComplication();
                Assert.assertEquals(complication.getHomologyForwardStrandRep(), expectedHomology);
                Assert.assertEquals(complication.getInsertedSequenceForwardStrandRep(), expectedInsertion);
            } else {
                final BreakpointComplications.SmallDuplicationBreakpointComplications complication =
                        (BreakpointComplications.SmallDuplicationBreakpointComplications) breakpoints.getComplication();
                Assert.assertEquals(complication.getHomologyForwardStrandRep(), expectedHomology);
                Assert.assertEquals(complication.getInsertedSequenceForwardStrandRep(), expectedInsertion);
                Assert.assertEquals(complication.getDupSeqRepeatUnitRefSpan(), expectedRepeatUnitRefSpan);
                Assert.assertEquals(complication.getDupSeqRepeatNumOnRef(), expectedRefDupNum);
                Assert.assertEquals(complication.getDupSeqRepeatNumOnCtg(), expectedCtgDupNum);
                Assert.assertEquals(complication.getCigarStringsForDupSeqOnCtg(), expectedTandupCigarStrings);
            }
        } else {
            final BreakpointComplications.IntraChrStrandSwitchBreakpointComplications complication =
                    (BreakpointComplications.IntraChrStrandSwitchBreakpointComplications) breakpoints.getComplication();
            Assert.assertEquals(complication.getHomologyForwardStrandRep(), expectedHomology);
            Assert.assertEquals(complication.getInsertedSequenceForwardStrandRep(), expectedInsertion);
            Assert.assertEquals(complication.getDupSeqRepeatUnitRefSpan(), expectedRepeatUnitRefSpan);
            Assert.assertEquals(complication.getDupSeqRepeatNumOnRef(), expectedRefDupNum);
            Assert.assertEquals(complication.getDupSeqRepeatNumOnCtg(), expectedCtgDupNum);
            Assert.assertEquals(complication.getCigarStringsForDupSeqOnCtg(), expectedTandupCigarStrings);
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Output out = new Output(bos);
        final Kryo kryo = new Kryo();
        kryo.writeClassAndObject(out, breakpoints);
        out.flush();

        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final Input in = new Input(bis);
        @SuppressWarnings("unchecked")
        final NovelAdjacencyAndInferredAltHaptype roundTrip = (NovelAdjacencyAndInferredAltHaptype) kryo.readClassAndObject(in);
        Assert.assertEquals(roundTrip, breakpoints);
    }


    /**
     * @see SVDiscoveryTestDataProvider#forComplexTandemDuplication()
     */
    @Test(groups = "sv")
    public void testGetBreakpoints_tandemDuplication_complex() {

        final String leftRefFlank       = "TGCCAGGTTACATGGCAAAGAGGGTAGATAT";                                                                    // 31
        final String rightRefFlank      = "TGGTGCAAATGCCATTTATGCTCCTCTCCACCCATATCC";                                                            // 39
        final String firstRepeat        = "GGGGAGCTGTGAAGAATGGAGCCAGTAATTAAATTCACTGAAGTCTCCACAGGAGGGCAAGGTGGACAATCTGTCCCATAGGAGGGGGATTCATGA";   // 96
        final String secondRepeat       = "GGGGAGCTGTGAAGAATGGAGCCAGTAATTAAATTCACTGAAGTCTCCACAGGAGGGCAAGGTGGACAATCTGTCCCATAGGAGGGGGATTCAGGA";   // 96
        final String pseudoHomology     = "GGGCAGCTGTGGA";                                                                                      // 13


        // first test (the original observed event, but assigned to a different chromosome): expansion from 1 unit to 2 units with pseudo-homology
        NovelAdjacencyAndInferredAltHaptype breakpoints = SVDiscoveryTestDataProvider.forComplexTanDup_1to2_pseudoHom_plus.biPathBubble;
        BreakpointComplications.SmallDuplicationBreakpointComplications complication =
                (BreakpointComplications.SmallDuplicationBreakpointComplications) breakpoints.getComplication();

        Assert.assertTrue(StringUtils.getLevenshteinDistance(complication.getHomologyForwardStrandRep(), pseudoHomology)<=2);
        Assert.assertTrue(complication.getInsertedSequenceForwardStrandRep().isEmpty());
        Assert.assertEquals(complication.getDupSeqRepeatUnitRefSpan(), new SimpleInterval("20", 312610, 312705));
        Assert.assertEquals(complication.getDupSeqRepeatNumOnRef(), 1);
        Assert.assertEquals(complication.getDupSeqRepeatNumOnCtg(), 2);
        Assert.assertEquals(breakpoints.getLeftJustifiedLeftRefLoc(), new SimpleInterval("20", 312609, 312609));
        Assert.assertEquals(breakpoints.getLeftJustifiedRightRefLoc(), new SimpleInterval("20", 312609, 312609));
        Assert.assertEquals(breakpoints.getStrandSwitch(), StrandSwitch.NO_SWITCH);
        Assert.assertEquals(breakpoints.getAltHaplotypeSequence(), (firstRepeat+secondRepeat+pseudoHomology).getBytes());
        NovelAdjacencyAndInferredAltHaptype breakpointsRev = SVDiscoveryTestDataProvider.forComplexTanDup_1to2_pseudoHom_minus.biPathBubble;
        Assert.assertEquals(breakpointsRev, breakpoints); // different representation, should lead to same result

        // second test: contraction from 2 units to 1 unit with pseudo-homology
        breakpoints = SVDiscoveryTestDataProvider.forComplexTanDup_2to1_pseudoHom_plus.biPathBubble;
        complication = (BreakpointComplications.SmallDuplicationBreakpointComplications) breakpoints.getComplication();
        Assert.assertTrue(StringUtils.getLevenshteinDistance(complication.getHomologyForwardStrandRep(), firstRepeat+pseudoHomology)<=2);
        Assert.assertTrue(complication.getInsertedSequenceForwardStrandRep().isEmpty());
        Assert.assertEquals(complication.getDupSeqRepeatUnitRefSpan(), new SimpleInterval("20", 312610, 312705));
        Assert.assertEquals(complication.getDupSeqRepeatNumOnRef(), 2);
        Assert.assertEquals(complication.getDupSeqRepeatNumOnCtg(), 1);
        Assert.assertEquals(breakpoints.getLeftJustifiedLeftRefLoc(), new SimpleInterval("20", 312609, 312609));
        Assert.assertEquals(breakpoints.getLeftJustifiedRightRefLoc(), new SimpleInterval("20", 312705, 312705));
        Assert.assertEquals(breakpoints.getStrandSwitch(), StrandSwitch.NO_SWITCH);
        Assert.assertEquals(breakpoints.getAltHaplotypeSequence(), (firstRepeat+pseudoHomology).getBytes());
        breakpointsRev = SVDiscoveryTestDataProvider.forComplexTanDup_2to1_pseudoHom_minus.biPathBubble;
        Assert.assertEquals(breakpointsRev, breakpoints); // different representation, should lead to same result

        // third test: contraction from 3 units to 2 units without pseudo-homology
        breakpoints = SVDiscoveryTestDataProvider.forComplexTanDup_3to2_noPseudoHom_plus.biPathBubble;
        complication = (BreakpointComplications.SmallDuplicationBreakpointComplications) breakpoints.getComplication();
        Assert.assertTrue(StringUtils.getLevenshteinDistance(complication.getHomologyForwardStrandRep(), firstRepeat+secondRepeat)<=2);
        Assert.assertTrue(complication.getInsertedSequenceForwardStrandRep().isEmpty());
        Assert.assertEquals(complication.getDupSeqRepeatUnitRefSpan(), new SimpleInterval("20", 312610, 312705));
        Assert.assertEquals(complication.getDupSeqRepeatNumOnRef(), 3);
        Assert.assertEquals(complication.getDupSeqRepeatNumOnCtg(), 2);
        Assert.assertEquals(breakpoints.getLeftJustifiedLeftRefLoc(), new SimpleInterval("20", 312609, 312609));
        Assert.assertEquals(breakpoints.getLeftJustifiedRightRefLoc(), new SimpleInterval("20", 312705, 312705));
        Assert.assertEquals(breakpoints.getStrandSwitch(), StrandSwitch.NO_SWITCH);
        Assert.assertEquals(breakpoints.getAltHaplotypeSequence(), (firstRepeat+secondRepeat).getBytes());
        breakpointsRev = SVDiscoveryTestDataProvider.forComplexTanDup_3to2_noPseudoHom_minus.biPathBubble;
        Assert.assertEquals(breakpointsRev, breakpoints); // different representation, should lead to same result

        // fourth test: expansion from 2 units to 3 units without pseudo-homology
        breakpoints = SVDiscoveryTestDataProvider.forComplexTanDup_2to3_noPseudoHom_plus.biPathBubble;
        complication = (BreakpointComplications.SmallDuplicationBreakpointComplications) breakpoints.getComplication();
        Assert.assertTrue(StringUtils.getLevenshteinDistance(complication.getHomologyForwardStrandRep(), firstRepeat)<=2);
        Assert.assertTrue(complication.getInsertedSequenceForwardStrandRep().isEmpty());
        Assert.assertEquals(complication.getDupSeqRepeatUnitRefSpan(), new SimpleInterval("20", 312610, 312705));
        Assert.assertEquals(complication.getDupSeqRepeatNumOnRef(), 2);
        Assert.assertEquals(complication.getDupSeqRepeatNumOnCtg(), 3);
        Assert.assertEquals(breakpoints.getLeftJustifiedLeftRefLoc(), new SimpleInterval("20", 312609, 312609));
        Assert.assertEquals(breakpoints.getLeftJustifiedRightRefLoc(), new SimpleInterval("20", 312609, 312609));
        Assert.assertEquals(breakpoints.getStrandSwitch(), StrandSwitch.NO_SWITCH);
        Assert.assertEquals(breakpoints.getAltHaplotypeSequence(), (firstRepeat+secondRepeat+firstRepeat).getBytes());
        breakpointsRev = SVDiscoveryTestDataProvider.forComplexTanDup_2to3_noPseudoHom_minus.biPathBubble;
        Assert.assertEquals(breakpointsRev, breakpoints); // different representation, should lead to same result

    }

    @Test(groups = "sv")
    public void testRefOrderSwitch() {
        AlignmentInterval region1 = new AlignmentInterval(
                // assigned from chr18 to chr21 to use the dict
                new SimpleInterval("chr21", 39477098, 39477363),
                1 ,268,
                TextCigarCodec.decode("236M2I30M108S"), true, 32, 25, 133, ContigAlignmentsModifier.AlnModType.NONE);
        AlignmentInterval region2 = new AlignmentInterval(
                new SimpleInterval("chr21", 39192594, 39192692),
                252 ,350,
                TextCigarCodec.decode("251S99M26S"), true, 32, 1, 94, ContigAlignmentsModifier.AlnModType.NONE);
        ChimericAlignment simpleChimera = new ChimericAlignment(region1, region2, Collections.emptyList(), "testContig", SVDiscoveryTestDataProvider.b38_seqDict);
        NovelAdjacencyAndInferredAltHaptype breakpoints = new NovelAdjacencyAndInferredAltHaptype(simpleChimera,
                "TTCCTTAAAATGCAGGTGAATACAAGAATTAGGTTTCAGGTTTTATATATATATTCTGATATATATATATAATATAACCTGAGATATATATATAAATATATATATTAATATATATTAATATATATAAATATATATATATTAATATATATTTATATATAAATATATATATATTAATATATATAAATATATATAAATATATATATATTAATATATATTAATATATAAATATATATATATTAATATATATTAATATATATAAATATATATATTAATATATATAAATATATATATAAATATATATAAATATATAAATATATATATAAATATATATAAATATATATAAATATATATACACACATACATACACATATACATT".getBytes(),
                SVDiscoveryTestDataProvider.b38_seqDict);
        Assert.assertEquals(breakpoints.getLeftJustifiedLeftRefLoc(), new SimpleInterval("chr21", 39192594, 39192594));
        Assert.assertEquals(breakpoints.getLeftJustifiedRightRefLoc(), new SimpleInterval("chr21", 39477346, 39477346));
        Assert.assertEquals(breakpoints.getComplication().getHomologyForwardStrandRep(), "ATATATAAATATATATA");
        Assert.assertTrue(breakpoints.getComplication().getInsertedSequenceForwardStrandRep().isEmpty());
    }
}