<#import "../../macros/supermatchLocation.ftl" as supermatchLocationMacro>
<#if condensedView??>
<#--<#assign superMatchId="0" scope="request">-->
    <#global superMatchId=0>

    <#list condensedView.lines as line>
        <#assign type=line.type>
    <li class="signature entry-signatures">
        <#-- the order of the divs is important , first right column fixed-->
        <div class="bot-row-line" style="margin: 0;">
            <div class="matches">
                <#list line.superMatchList as superMatch>
                    <#global superMatchId=superMatchId + 1>
                    <#--This check ensures that the entry is in the colour mapping file.  If not, uses a default class.-->
                    <#if entryColours[superMatch.firstEntry.ac]??>
                    <@supermatchLocationMacro.supermatchLocation smid=superMatchId proteinLength=proteinLength supermatch=superMatch colourClass="c"+entryColours[superMatch.firstEntry.ac]+" "+type />
                <#else>
                    <@supermatchLocationMacro.supermatchLocation smid=superMatchId proteinLength=proteinLength supermatch=superMatch colourClass="c1 "+type />
                </#if>
                </#list>
            </div>
        </div>
    </li>
    </#list>
</#if>
