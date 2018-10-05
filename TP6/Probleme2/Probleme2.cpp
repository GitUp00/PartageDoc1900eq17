// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */

// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)

#include <avr/interrupt.h>
#include <avr/io.h>

#define F_CPU 8000000
#include <util/delay.h>
volatile uint8_t minuterieExpiree = 0; //-----p2
volatile uint8_t boutonPoussoir = 0; 
volatile uint8_t compteur = 0;
uint8_t rouge = 0b10; //Led prend la couleur rouge	
uint8_t vert = 0b01; //Led prend la couleur verte




ISR(TIMER1_COMPA_vect)
{
    boutonPoussoir = 0;
}


ISR(INT0_vect)
{
	if(boutonPoussoir == 0){
	//partirMinuterie(86000);
	boutonPoussoir =1;
	//PORTA = rouge;
	}

	else if(boutonPoussoir == 1){
	boutonPoussoir = 0;
	} 
}
int main()
{
	cli();
    DDRA = 0xff; // PORT A est en mode sortie
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree
    
	DDRD = 1<<INT0;
    PORTD = 1<<INT0;
    PORTA = 0 << INT0;
    EIMSK |= (1 << INT0) ;
    EICRA |= (1 << ISC00);
    
    sei();


    


