package uk.ac.ebi.interpro.scan.io.match.writer;

import org.apache.log4j.Logger;
import uk.ac.ebi.interpro.scan.io.sequence.XrefParser;
import uk.ac.ebi.interpro.scan.model.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * Write matches as output in GFF (Generic Feature Format) version 3. This writer is specific
 * for nucleotide sequence scans (back tracking from protein match to nucleotide sequence).
 * <p/>
 * GFF3 description (http://www.sequenceontology.org/gff3.shtml):
 * The format consists of 9 columns, separated by tabs (NOT spaces).
 *
 * @author Maxim Scheremetjew, EMBL-EBI, InterPro
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class GFFResultWriterForNucSeqs extends ProteinMatchesGFFResultWriter {

    private static final Logger LOGGER = Logger.getLogger(GFFResultWriterForNucSeqsBackup.class.getName());
    private String nucleotideId;

    /**
     * For testing only *
     */
    protected GFFResultWriterForNucSeqs() {
        super();
    }

    public GFFResultWriterForNucSeqs(File file) throws IOException {
        super(file);
    }

    private String getNucleotideId() {
        return nucleotideId;
    }

    private void setNucleotideId(String nucleotideId) {
        this.nucleotideId = super.getValidGFF3SeqId(nucleotideId);
    }

    /**
     * Writes out a Protein object to a GFF version 3 file
     *
     * @param protein containing matches to be written out
     * @return the number of rows printed (i.e. the number of Locations on Matches).
     * @throws java.io.IOException in the event of I/O problem writing out the file.
     */
    public int write(Protein protein) throws IOException {
        String proteinIdFromGetorf = getProteinAccession(protein);
        int sequenceLength = protein.getSequenceLength();
        String md5 = protein.getMd5();
        String date = dmyFormat.format(new Date());
        Set<Match> matches = protein.getMatches();
        String proteinIdForGFF = null;
        if (matches.size() > 0) {
            proteinIdFromGetorf = super.getValidGFF3SeqId(proteinIdFromGetorf);
            //Write nucleotide sequences and ORFs
            //Write nucleic acid and returns ORF of interest
            writeSequenceRegionPart(protein, sequenceLength, md5, proteinIdForGFF, proteinIdFromGetorf);
            processMatches(matches, proteinIdForGFF, date, protein, getNucleotideId());

        }
        return 0;
    }

    /**
     * 1. Handles nucleic acid with multiple cross references (same sequence, different identifiers).
     * Example
     * ##sequence-region Wilf|A2YIW7 1 366
     * Wilf|A2YIW7	provided_by_user	nucleic_acid	1	366	.	+	.	Name=Wilf|A2YIW7;md5=e9b174d63adc63bab79c90fdbc8d1670;ID=Wilf|A2YIW7
     * Wilf|A2YIW7	getorf	ORF	1	366	.	+	.	Name=Wilf_5|A2YIW7_5;Target=pep_Wilf|A2YIW7_1_366 1 122;md5=e9b174d63adc63bab79c90fdbc8d1670;ID=orf_Wilf|A2YIW7_1_366
     * Wilf|A2YIW7	getorf	polypeptide	1	122	.	+	.	md5=f927b0d241297dcc9a1c5990b58bf3c4;ID=pep_Wilf|A2YIW7_1_366
     * <p/>
     * 2. And relations between different nucleic sequences sharing the same orf/protein
     * <p/>
     * I.   Iterate over all protein ORFs
     * II.  Get nucleic acids
     * III. Iterate over all nucleic acids xrefs
     * IV.  Iterate over all protein xrefs
     * V.   Compare protein xrefs with nucleic acid xrefs
     * VI.  If they match build a concatenation of nucleic acids separated by pipes
     * VII. If you finished iteration over all nucleic acids xrefs write sequence region to output
     *
     * @param protein
     * @throws IOException
     */
    private void writeSequenceRegionPart(final Protein protein, final int sequenceLength, final String md5,
                                         String proteinIdForGFF, final String proteinIdFromGetorf) throws IOException {
        // I.
        for (OpenReadingFrame orf : protein.getOpenReadingFrames()) {
            // II.
            final NucleotideSequence nucleotideSequence = orf.getNucleotideSequence();
            final StringBuilder concatenatedNucSeqIdentifiers = new StringBuilder();
            // III.
            for (final NucleotideSequenceXref nucleotideSequenceXref : nucleotideSequence.getCrossReferences()) {
                String nucleotideSequenceXrefId = nucleotideSequenceXref.getIdentifier();
                // IV.
                for (ProteinXref proteinXref : protein.getCrossReferences()) {
                    // Getorf appends '_N' where N is an integer to the protein accession. We need to compare this to the nucleotide sequence ID, that does not have _N on the end, so first of all strip this off for the comparison.
                    String strippedProteinId = XrefParser.stripOfFinalUnderScore(proteinXref.getIdentifier());
                    // Get rid of those pesky version numbers too.
                    strippedProteinId = XrefParser.stripOfVersionNumberIfExists(strippedProteinId);
                    // V.
                    if ((nucleotideSequenceXrefId.equals(strippedProteinId))) {
                        // VI.
                        if (concatenatedNucSeqIdentifiers.length() > 0) {
                            concatenatedNucSeqIdentifiers.append(VALUE_SEPARATOR);
                        }
                        concatenatedNucSeqIdentifiers.append(nucleotideSequenceXrefId);
                    }
                }
            }
            // VII.
            String concatenatedNucSeqIdentifiersStr = concatenatedNucSeqIdentifiers.toString();
            if (concatenatedNucSeqIdentifiersStr.length() > 0) {
                setNucleotideId(concatenatedNucSeqIdentifiersStr);
                super.gffWriter.write("##sequence-region " + concatenatedNucSeqIdentifiersStr + " 1 " + nucleotideSequence.getSequence().length());
                super.gffWriter.write(getNucleicAcidLine(nucleotideSequence));
                //Build protein identifier for GFF3
                proteinIdForGFF = buildProteinIdentifier(orf);
                proteinIdForGFF = super.getValidGFF3SeqId(proteinIdForGFF);

                //Write sequence to the FASTA part
                addFASTASeqToMap(proteinIdForGFF, protein.getSequence());
                //Write ORF
                super.gffWriter.write(getORFLine(orf, proteinIdFromGetorf, proteinIdForGFF, sequenceLength));
                //Write polypeptide
                super.gffWriter.write(getPolypeptideLine(sequenceLength, proteinIdForGFF, md5));
            } else {
                throw new IllegalStateException("Cannot find the ORF object that maps to protein with PK / MD5: " + protein.getId() + " / " + protein.getMd5());
            }
        }
    }

    private List<String> getORFLine(OpenReadingFrame orf, String proteinIdFromGetorf, String proteinIdForGFF, int proteinLength) {
        if (orf == null) {
            throw new IllegalArgumentException("A null orf has been passed in.");
        }
        final String seqId = getNucleotideId();
        final String strand = (NucleotideSequenceStrand.SENSE.equals(orf.getStrand()) ? "+" : "-");
        final String orfIdentifier = buildOrfIdentifier(orf);
        GFF3Feature orfFeature = new GFF3Feature(seqId, "getorf", "ORF", orf.getStart(), orf.getEnd(), strand);
        orfFeature.addAttribute(GFF3Feature.ID_ATTR, orfIdentifier);
        orfFeature.addAttribute(GFF3Feature.NAME_ATTR, proteinIdFromGetorf);
        orfFeature.addAttribute(GFF3Feature.TARGET_ATTR, proteinIdForGFF + " 1" + " " + proteinLength);
        NucleotideSequence ntSeq = orf.getNucleotideSequence();
        if (orf.getNucleotideSequence() != null) {
            orfFeature.addAttribute(GFF3Feature.MD5_ATTR, ntSeq.getMd5());
        }
        return orfFeature.getGFF3FeatureLine();
    }

    /**
     * Generates an ORF identifier used in GFF3.
     */
    private String buildOrfIdentifier(OpenReadingFrame orf) {
        return new StringBuilder("orf_").append(getIdentifierSuffix(orf)).toString();
    }

    /**
     * Generates an protein identifier used in GFF3.
     */
    private String buildProteinIdentifier(OpenReadingFrame orf) {
        return new StringBuilder("pep_").append(getIdentifierSuffix(orf)).toString();
    }

    private String getIdentifierSuffix(OpenReadingFrame orf) {
        if (orf == null) {
            LOGGER.warn("Called GFFResultWriterForNucSeqs.getIdentifierSuffix() with a null ORF???");
            return "";
        }
        StringBuilder sb = new StringBuilder(getNucleotideId());
        sb
                .append("_")
                .append(orf.getStart())
                .append("_")
                .append(orf.getEnd())
                .append((NucleotideSequenceStrand.ANTISENSE.equals(orf.getStrand()) ? "_r" : ""));
        return sb.toString();
    }

    private List<String> getNucleicAcidLine(NucleotideSequence nucleotideSeq) {
        String seqId = getNucleotideId();
        int end = nucleotideSeq.getSequence().length();
        GFF3Feature nucleicAcidFeature = new GFF3Feature(seqId, "provided_by_user", "nucleic_acid", 1, end, "+");
        nucleicAcidFeature.addAttribute(GFF3Feature.ID_ATTR, getNucleotideId());
        nucleicAcidFeature.addAttribute(GFF3Feature.NAME_ATTR, getNucleotideId());
        nucleicAcidFeature.addAttribute(GFF3Feature.MD5_ATTR, nucleotideSeq.getMd5());
        return nucleicAcidFeature.getGFF3FeatureLine();
    }

    /**
     * Writes information about the target protein sequence (or reference sequence).
     */
    private List<String> getPolypeptideLine(int sequenceLength, String proteinIdForGFF, String md5) {
        String seqId = getNucleotideId();
        GFF3Feature polypeptideFeature = new GFF3Feature(seqId, "getorf", "polypeptide", 1, sequenceLength, "+");
        polypeptideFeature.addAttribute(GFF3Feature.ID_ATTR, proteinIdForGFF);
        polypeptideFeature.addAttribute(GFF3Feature.MD5_ATTR, md5);
        return polypeptideFeature.getGFF3FeatureLine();
    }
}