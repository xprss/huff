export function HexatrapTutorial() {
  return (
    <details className="trap-panel trap-help">
      <summary>Come si gioca</summary>
      <ol>
        <li>Due interlocutori: uno dice <strong>sempre</strong> la verità, l'altro mente <strong>sempre</strong>. Ogni giorno un caso nuovo, scritto nella notte.</li>
        <li>Certezze e deposizioni sono gratis. Le domande (sì/no) e i reperti costano minuti: ne hai 60, non bastano per tutto.</li>
        <li>Un reperto è sempre vero. Una risposta del sincero è vera, quella del bugiardo è falsa.</li>
        <li>Sotto ogni frase trovi il <strong>verbale</strong>: la formulazione esatta che fa fede.</li>
        <li>Quando sei sicuro emetti il verdetto: chi mente e come sono andati i fatti richiesti. Si può emettere una volta sola.</li>
      </ol>
      <p><strong>Esempio.</strong> Ada dice: «La porta era chiusa». Un reperto prova che era aperta. Allora Ada mente, e quindi l'altro dice il vero: ogni sua frase diventa un fatto certo.</p>
      <p>La <strong>lente delle ipotesi</strong> mostra cosa implicherebbe ogni frase se mentisse l'uno o l'altro: cerca l'ipotesi che porta a una contraddizione.</p>
    </details>
  );
}
